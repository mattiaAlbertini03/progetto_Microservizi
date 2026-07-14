package me.msaproject.fleetservice.dto;

public record VehicleTrackingResponse(
        String vehicleId,
        double lat,
        double lon,
        long timestampEpochMs
) {
}