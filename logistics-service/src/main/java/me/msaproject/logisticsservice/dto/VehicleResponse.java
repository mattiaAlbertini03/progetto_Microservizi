package me.msaproject.logisticsservice.dto;

public record VehicleResponse(
        String id,
        String type,
        Double lat,
        Double lon,
        VehicleStatus status,
        int batteryPct,
        String qrCode
) {}