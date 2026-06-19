package me.msaproject.vehiclesimulatorservice;

public record VehicleTrackingEvent(
        String vehicleId,
        double lat,
        double lon,
        Long timestampEpochMs
) {}
