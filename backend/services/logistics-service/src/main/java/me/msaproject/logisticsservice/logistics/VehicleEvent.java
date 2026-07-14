package me.msaproject.logisticsservice.logistics;

public record VehicleEvent(
        String type,
        String vehicleId
) {}