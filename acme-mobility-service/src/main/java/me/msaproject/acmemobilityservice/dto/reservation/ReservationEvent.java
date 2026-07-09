package me.msaproject.acmemobilityservice.dto.reservation;

public record ReservationEvent(
        String reservationId,
        String status,
        String message
) {}