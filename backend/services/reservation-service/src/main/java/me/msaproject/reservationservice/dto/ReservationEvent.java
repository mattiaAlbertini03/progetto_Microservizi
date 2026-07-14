package me.msaproject.reservationservice.dto;

public record ReservationEvent(
        String reservationId,
        String status,
        String message
) {}