package me.msaproject.reservationservice.reservation;

public record Reservation(
        String reservationId,
        String userId,
        String vehicleId,
        String stationId,
        String status
) {}