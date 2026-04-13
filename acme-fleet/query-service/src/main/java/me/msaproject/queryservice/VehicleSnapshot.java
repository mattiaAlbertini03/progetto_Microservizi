package me.msaproject.queryservice;

public record VehicleSnapshot(
    String vehicleId,
    Double lat,
    Double lon,
    Long trackingTimestampEpochMs,
    Double batteryPercent,
    Long batteryTimestampEpochMs
) {}
