package me.msaproject.acmemobilityservice.dto.reservation;

public record ReservationEvent(
        String requestId,
        String reservationId,
        String status,
        String message
) {}