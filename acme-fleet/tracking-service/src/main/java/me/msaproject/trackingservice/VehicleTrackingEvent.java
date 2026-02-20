package me.msaproject.trackingservice;

public record VehicleTrackingEvent(
        String vehicleId,
        double lat,
        double lon,
        Long timestampEpochMs
) {}
