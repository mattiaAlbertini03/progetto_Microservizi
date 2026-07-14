package me.msaproject.acmemobilityservice.dto.reservation;

public record ReservationCreationEvent(
        String requestId,
        String status,
        String message
) {}