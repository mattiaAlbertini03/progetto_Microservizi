package me.msaproject.acmemobilityservice.dto.reservation;

public record ReservationCreationEvent(
        String requestId,
        String reservationId,
        String status,
        String message
) {}