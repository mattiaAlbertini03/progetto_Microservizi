package me.msaproject.logisticsservice.dto.logistics;

public record VehicleStatusResponse(
        String vehicleId,
        VehicleStatus status,
        int batteryPct
) {}