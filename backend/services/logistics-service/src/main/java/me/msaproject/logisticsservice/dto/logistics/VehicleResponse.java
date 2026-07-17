package me.msaproject.logisticsservice.dto.logistics;

public record VehicleResponse(
        String id,
        String name,
        String type,
        Double lat,
        Double lon,
        VehicleStatus status,
        int batteryPct,
        String qrCode
) {}
