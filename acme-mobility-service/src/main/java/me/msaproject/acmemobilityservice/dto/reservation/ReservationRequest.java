package me.msaproject.acmemobilityservice.dto.reservation;

public record ReservationRequest(
        String userId,
        String vehicleId,
        String stationId
) {}