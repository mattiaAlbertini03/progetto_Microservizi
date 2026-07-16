package me.msaproject.simulatorservice.dto;

public record SimulationSummary(
        String vehicleId,
        int minutesUsed,
        double kilometers,
        double batteryPct
) {}
