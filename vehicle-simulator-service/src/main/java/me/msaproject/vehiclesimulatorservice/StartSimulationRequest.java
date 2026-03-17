package me.msaproject.vehiclesimulatorservice;

public record StartSimulationRequest(
        String vehicleId,
        String fromStationId,
        String toStationId,
        double fromLat,
        double fromLon,
        double toLat,
        double toLon,
        double speedMetersPerSecond,
        double initialBatteryPct
) {}