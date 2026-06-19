package me.msaproject.acmemobilityservice.dto.reservation;

public record ReservationRequest(
        String requestId,
        String userId,
        String vehicleId,
        String stationId
) {}