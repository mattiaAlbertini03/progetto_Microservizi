package me.msaproject.fleetservice.dto;

public record Reservation(
        String reservationId,
        String userId,
        String vehicleId,
        String stationId,
        String status
) {}