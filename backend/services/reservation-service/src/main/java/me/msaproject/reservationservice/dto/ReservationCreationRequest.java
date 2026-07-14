package me.msaproject.reservationservice.dto;

public record ReservationCreationRequest(
        String userId,
        String vehicleId,
        String stationId
) {}