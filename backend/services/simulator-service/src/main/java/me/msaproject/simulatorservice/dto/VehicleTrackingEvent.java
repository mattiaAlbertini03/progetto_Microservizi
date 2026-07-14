package me.msaproject.simulatorservice.dto;

public record VehicleTrackingEvent(
        String vehicleId,
        double lat,
        double lon,
        Long timestampEpochMs
) {}
