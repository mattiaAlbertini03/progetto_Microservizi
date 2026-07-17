package me.msaproject.fleetservice.dto;

public record SimulationSummary(
        String vehicleId,
        int minutesUsed,
        double kilometers,
        double batteryPct,
        double minimumBatteryPct
) {}
