package me.msaproject.acmemobilityservice.dto.reservation;

public record ReservationStatusResponse(
        String reservationId,
        String status
) {}