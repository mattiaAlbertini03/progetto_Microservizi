package me.msaproject.acmemobilityservice.dto.reservation;

public record ReservationActionRequest(
        String reservationId,
        String userId,
        String vehicleId,
        String stationId,
        boolean lateCancellation
) {}
