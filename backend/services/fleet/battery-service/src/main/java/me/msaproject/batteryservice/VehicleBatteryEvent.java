package me.msaproject.batteryservice;

public record VehicleBatteryEvent(
        String vehicleId,
        double batteryPct,
        Long timestampEpochMs
) {}
