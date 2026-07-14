package me.msaproject.acmemobilityservice.dto.reservation;

public record ReservationRequestStatusResponse(
        String requestId,
        String userId,
        String status
) {}