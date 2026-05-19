package me.msaproject.logisticsservice.dto;

public record VehicleStatusResponse(
        String vehicleId,
        VehicleStatus status,
        int batteryPct
) {}