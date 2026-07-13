package me.msaproject.acmemobilityservice.sse.reservation;

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
public class ReservationEventService {

    private final Map<String, List<SseEventSink>> subscribers = new ConcurrentHashMap<>();

    @Context
    Sse sse;

    public void subscribe(String topicId, SseEventSink sink) {
        subscribers
                .computeIfAbsent(topicId, key -> new CopyOnWriteArrayList<>())
                .add(sink);
    }

    public void publish(String topicId, Object event) {
        List<SseEventSink> sinks = subscribers.get(topicId);

        if (sinks == null) {
            return;
        }

        sinks.removeIf(SseEventSink::isClosed);

        for (SseEventSink sink : sinks) {
            try {
                sink.send(
                        sse.newEventBuilder()
                                .name("reservation-update")
                                .mediaType(MediaType.APPLICATION_JSON_TYPE)
                                .data(event.getClass(), event)
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
            subscribers.remove(topicId);
        }
    }
}