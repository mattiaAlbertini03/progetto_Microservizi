package me.msaproject.reservationservice.dto;

public record ReservationCreationRequest(
        String requestId,
        String userId,
        String vehicleId,
        String stationId,
        long scheduledStartEpochMs
) {}
