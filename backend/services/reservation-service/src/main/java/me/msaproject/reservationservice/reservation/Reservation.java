package me.msaproject.reservationservice.reservation;

public record Reservation(
        String id,
        String userId,
        String vehicleId,
        String stationId,
        String status
) {}