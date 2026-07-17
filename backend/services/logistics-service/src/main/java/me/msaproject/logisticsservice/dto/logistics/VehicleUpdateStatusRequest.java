package me.msaproject.logisticsservice.dto.logistics;

public record VehicleUpdateStatusRequest(
    double lat,
    double lon,
    int batteryPct
) {}