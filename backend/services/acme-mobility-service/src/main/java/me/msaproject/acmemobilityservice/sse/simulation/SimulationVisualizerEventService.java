package me.msaproject.acmemobilityservice.sse.simulation;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;
import me.msaproject.acmemobilityservice.dto.reservation.SimulationVisualizerEvent;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@ApplicationScoped
public class SimulationVisualizerEventService {

    private static final String EVENT_NAME = "simulation-update";
    private static final long RECONNECT_DELAY_MILLIS = 3_000;

    // This demo runs one ACME Mobility replica. A production deployment with
    // multiple replicas should replace this map with a shared event broker.
    private final Map<String, List<SseEventSink>> subscribers = new ConcurrentHashMap<>();

    @Context
    Sse sse;

    public void subscribe(String reservationId, SseEventSink sink) {
        subscribers
                .computeIfAbsent(reservationId, key -> new CopyOnWriteArrayList<>())
                .add(sink);

        try {
            // Commit the response immediately so the browser knows that it can
            // safely receive the next BPMN update.
            sink.send(sse.newEventBuilder()
                    .comment("connected")
                    .reconnectDelay(RECONNECT_DELAY_MILLIS)
                    .build());
        } catch (Exception exception) {
            remove(reservationId, sink);
            closeQuietly(sink);
        }
    }

    public void publish(String reservationId, SimulationVisualizerEvent event) {
        List<SseEventSink> sinks = subscribers.get(reservationId);
        if (sinks == null) {
            return;
        }

        sinks.removeIf(SseEventSink::isClosed);
        for (SseEventSink sink : sinks) {
            try {
                sink.send(sse.newEventBuilder()
                        .name(EVENT_NAME)
                        .mediaType(MediaType.APPLICATION_JSON_TYPE)
                        .data(SimulationVisualizerEvent.class, event)
                        .build());
            } catch (Exception exception) {
                closeQuietly(sink);
            }
        }

        sinks.removeIf(SseEventSink::isClosed);
        if (sinks.isEmpty()) {
            subscribers.remove(reservationId, sinks);
        }
    }

    private void remove(String reservationId, SseEventSink sink) {
        List<SseEventSink> sinks = subscribers.get(reservationId);
        if (sinks == null) {
            return;
        }

        sinks.remove(sink);
        if (sinks.isEmpty()) {
            subscribers.remove(reservationId, sinks);
        }
    }

    private void closeQuietly(SseEventSink sink) {
        try {
            sink.close();
        } catch (Exception ignored) {
            // The client is already gone; no further cleanup is required.
        }
    }
}
