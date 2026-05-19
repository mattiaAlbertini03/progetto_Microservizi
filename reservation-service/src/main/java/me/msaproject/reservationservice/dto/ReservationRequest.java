package me.msaproject.reservationservice.dto;

public record ReservationRequest(
        String userId,
        String vehicleId,
        String stationId
) {}