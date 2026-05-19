package me.msaproject.acmemobilityservice.sse;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;
import me.msaproject.acmemobilityservice.dto.reservation.ReservationEvent;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@ApplicationScoped
public class ReservationEventService {

    private final Map<String, List<SseEventSink>> subscribers = new ConcurrentHashMap<>();

    @Inject
    Sse sse;

    public void subscribe(String reservationId, SseEventSink sink) {
        subscribers
                .computeIfAbsent(reservationId, key -> new CopyOnWriteArrayList<>())
                .add(sink);
    }

    public void publish(String reservationId, ReservationEvent event) {
        List<SseEventSink> sinks = subscribers.get(reservationId);

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
                                .data(ReservationEvent.class, event)
                                .build()
                );
            } catch (Exception e) {
                try {
                    sink.close();
                } catch (Exception ignored) {
                }
            }
        }

        sinks.removeIf(SseEventSink::isClosed);

        if (sinks.isEmpty()) {
            subscribers.remove(reservationId);
        }
    }
}