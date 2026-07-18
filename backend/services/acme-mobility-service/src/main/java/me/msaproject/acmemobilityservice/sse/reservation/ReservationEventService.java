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

    private static final System.Logger LOGGER = System.getLogger(ReservationEventService.class.getName());
    private final Map<String, List<SseEventSink>> subscribers = new ConcurrentHashMap<>();
    private final Map<String, Object> latestEvents = new ConcurrentHashMap<>();

    @Context
    Sse sse;

    public void subscribe(String topicId, SseEventSink sink) {
        subscribers
                .computeIfAbsent(topicId, key -> new CopyOnWriteArrayList<>())
                .add(sink);

        try {
            // Commit the SSE response immediately so EventSource can report that
            // the subscription is open before the workflow publishes an update.
            sink.send(
                    sse.newEventBuilder()
                            .comment("connected")
                            .reconnectDelay(3_000)
                            .build());

            Object latestEvent = latestEvents.get(topicId);
            if (latestEvent != null) {
                sendEvent(sink, latestEvent);
                LOGGER.log(System.Logger.Level.INFO,
                        "Replayed latest reservation event for " + topicId);
            }
        } catch (Exception exception) {
            removeSubscriber(topicId, sink);
            try {
                sink.close();
            } catch (Exception ignored) {
            }
        }
    }

    public void publish(String topicId, Object event) {
        latestEvents.put(topicId, event);
        List<SseEventSink> sinks = subscribers.get(topicId);

        if (sinks == null) {
            LOGGER.log(System.Logger.Level.INFO,
                    "Stored reservation event for late subscriber on " + topicId);
            return;
        }

        sinks.removeIf(SseEventSink::isClosed);

        for (SseEventSink sink : sinks) {
            try {
                sendEvent(sink, event);
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

    private void sendEvent(SseEventSink sink, Object event) {
        sink.send(
                sse.newEventBuilder()
                        .name("reservation-update")
                        .mediaType(MediaType.APPLICATION_JSON_TYPE)
                        .data(event.getClass(), event)
                        .build());
    }

    private void removeSubscriber(String topicId, SseEventSink sink) {
        List<SseEventSink> sinks = subscribers.get(topicId);

        if (sinks == null) {
            return;
        }

        sinks.remove(sink);

        if (sinks.isEmpty()) {
            subscribers.remove(topicId, sinks);
        }
    }
}
