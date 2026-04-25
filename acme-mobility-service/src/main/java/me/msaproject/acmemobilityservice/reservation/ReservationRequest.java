package me.msaproject.acmemobilityservice.reservation;

public record ReservationRequest(
        String userId,
        String vehicleId,
        String stationId
) {}