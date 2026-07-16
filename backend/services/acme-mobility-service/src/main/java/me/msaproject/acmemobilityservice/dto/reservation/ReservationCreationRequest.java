package me.msaproject.acmemobilityservice.dto.reservation;

public record ReservationCreationRequest(
        String requestId,
        String userId,
        String vehicleId,
        String stationId,
        long scheduledStartEpochMs
) {}
