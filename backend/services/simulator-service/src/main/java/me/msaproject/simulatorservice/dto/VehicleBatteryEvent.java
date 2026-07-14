package me.msaproject.simulatorservice.dto;

public record VehicleBatteryEvent(
        String vehicleId,
        double batteryPct,
        Long timestampEpochMs
) {}
