package me.msaproject.acmemobilityservice.reservation;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@ApplicationScoped
public class ReservationEventService {

    private final Map<String, List<SseEventSink>> subscribers = new ConcurrentHashMap<>();

    public void subscribe(String reservationId, SseEventSink sink) {
        subscribers
                .computeIfAbsent(reservationId, key -> new CopyOnWriteArrayList<>())
                .add(sink);
    }

    public void publish(String reservationId, ReservationEvent event, Sse sse) {
        List<SseEventSink> sinks = subscribers.get(reservationId);

        if (sinks == null) {
            return;
        }

        sinks.removeIf(SseEventSink::isClosed);

        for (SseEventSink sink : sinks) {
            sink.send(
                    sse.newEventBuilder()
                            .name("reservation-update")
                            .mediaType(jakarta.ws.rs.core.MediaType.APPLICATION_JSON_TYPE)
                            .data(ReservationEvent.class, event)
                            .build()
            );
        }
    }
}