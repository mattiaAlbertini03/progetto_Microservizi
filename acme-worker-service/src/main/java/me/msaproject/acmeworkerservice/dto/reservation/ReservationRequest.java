package me.msaproject.acmeworkerservice.dto.reservation;

public record ReservationRequest(
        String requestId,
        String userId,
        String vehicleId,
        String stationId
) {}