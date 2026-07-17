package me.msaproject.fleetservice.dto;

public record FleetStatusResponse(
        String reservationId,
        String vehicleId,
        double lat,
        double lon,
        int batteryPercent,
        String movementStatus,
        double kilometers,
        int minutesUsed,
        long trackingTimestampEpochMs,
        long batteryTimestampEpochMs
) {
}
