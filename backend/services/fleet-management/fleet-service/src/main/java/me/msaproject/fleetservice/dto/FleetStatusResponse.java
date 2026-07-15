package me.msaproject.fleetservice.dto;

public record FleetStatusResponse(
        String reservationId,
        String vehicleId,
        double lat,
        double lon,
        int batteryPercent,
        long trackingTimestampEpochMs,
        long batteryTimestampEpochMs
) {
}