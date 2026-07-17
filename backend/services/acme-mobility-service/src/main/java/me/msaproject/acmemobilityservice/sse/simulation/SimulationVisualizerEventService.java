package me.msaproject.acmemobilityservice.sse.simulation;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@ApplicationScoped
public class SimulationVisualizerEventService {

    private final Map<String, List<SseEventSink>> subscribers = new ConcurrentHashMap<>();

    @Context
    Sse sse;

    public void subscribe(String reservationId, SseEventSink sink) {
        subscribers.computeIfAbsent(reservationId, key -> new CopyOnWriteArrayList<>()).add(sink);
        try {
            sink.send(sse.newEventBuilder().comment("connected").reconnectDelay(3_000).build());
        } catch (Exception exception) {
            remove(reservationId, sink);
        }
    }

    public void publish(String reservationId, Object event) {
        List<SseEventSink> sinks = subscribers.get(reservationId);
        if (sinks == null) return;

        sinks.removeIf(SseEventSink::isClosed);
        for (SseEventSink sink : sinks) {
            try {
                sink.send(sse.newEventBuilder()
                        .name("simulation-update")
                        .mediaType(MediaType.APPLICATION_JSON_TYPE)
                        .data(event.getClass(), event)
                        .build());
            } catch (Exception exception) {
                try {
                    sink.close();
                } catch (Exception ignored) {
                }
            }
        }
        sinks.removeIf(SseEventSink::isClosed);
        if (sinks.isEmpty()) subscribers.remove(reservationId);
    }

    private void remove(String reservationId, SseEventSink sink) {
        List<SseEventSink> sinks = subscribers.get(reservationId);
        if (sinks == null) return;
        sinks.remove(sink);
        if (sinks.isEmpty()) subscribers.remove(reservationId, sinks);
    }
}
