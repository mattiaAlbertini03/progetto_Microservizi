package me.msaproject.fleetservice.dto;

public record Reservation(
        String id,
        String userId,
        String vehicleId,
        String stationId,
        String status
) {}