package me.msaproject.vehiclesimulatorservice;

public record VehicleBatteryEvent(
        String vehicleId,
        double batteryPct,
        Long timestampEpochMs
) {}
