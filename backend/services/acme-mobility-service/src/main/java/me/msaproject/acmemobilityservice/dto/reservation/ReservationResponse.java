package me.msaproject.acmemobilityservice.dto.reservation;


public record ReservationResponse(
        String reservationId,
        String userId,
        String vehicleId,
        String stationId,
        String status
) {}