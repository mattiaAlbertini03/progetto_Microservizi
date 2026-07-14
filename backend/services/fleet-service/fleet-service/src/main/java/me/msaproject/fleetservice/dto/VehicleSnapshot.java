package me.msaproject.fleetservice.dto;

public record VehicleSnapshot(
        String vehicleId,
        double lat,
        double lon,
        int batteryPercent,
        long trackingTimestampEpochMs,
        long batteryTimestampEpochMs
) {
}