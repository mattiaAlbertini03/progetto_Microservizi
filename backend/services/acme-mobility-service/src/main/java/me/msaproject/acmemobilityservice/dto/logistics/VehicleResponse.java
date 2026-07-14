package me.msaproject.acmemobilityservice.dto.logistics;

public record VehicleResponse(
        String id,
        String type,
        Double lat,
        Double lon,
        String status,
        int batteryPct,
        String qrCode
) {}