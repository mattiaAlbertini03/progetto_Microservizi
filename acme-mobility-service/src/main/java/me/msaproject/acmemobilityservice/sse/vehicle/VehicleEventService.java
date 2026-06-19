package me.msaproject.acmemobilityservice.sse.vehicle;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;
import me.msaproject.acmemobilityservice.dto.logistics.VehicleEvent;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@ApplicationScoped
public class VehicleEventService {

    private static final String GLOBAL = "global";

    private final Map<String, List<SseEventSink>> subscribers = new ConcurrentHashMap<>();

    @Context
    Sse sse;

    public void subscribe(String stationId, SseEventSink sink) {
        subscribers
                .computeIfAbsent(stationId, key -> new CopyOnWriteArrayList<>())
                .add(sink);
    }

    public void publish(String stationId, VehicleEvent event) {
        publishTo(stationId, event);
        publishTo(GLOBAL, event);
    }

    private void publishTo(String key, VehicleEvent event) {
        List<SseEventSink> sinks = subscribers.get(key);

        if (sinks == null) {
            return;
        }

        sinks.removeIf(SseEventSink::isClosed);

        for (SseEventSink sink : sinks) {
            try {
                sink.send(
                        sse.newEventBuilder()
                                .name("vehicle-update")
                                .mediaType(MediaType.APPLICATION_JSON_TYPE)
                                .data(VehicleEvent.class, event)
                                .build());
            } catch (Exception e) {
                try {
                    sink.close();
                } catch (Exception ignored) {
                }
            }
        }

        sinks.removeIf(SseEventSink::isClosed);

        if (sinks.isEmpty()) {
            subscribers.remove(key);
        }
    }
}