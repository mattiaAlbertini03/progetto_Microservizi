package me.msaproject.reservationservice.dto;

public record ReservationStatusResponse(
        String reservationId,
        String status
) {}