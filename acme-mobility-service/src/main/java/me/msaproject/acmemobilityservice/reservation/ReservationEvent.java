package me.msaproject.acmemobilityservice.reservation;

public record ReservationEvent(
        String reservationId,
        String status,
        String message
) {}