package me.msaproject.acmemobilityservice.vehicle;

public record Vehicle(
        String id,
        String type,
        String stationId,
        String status,
        int batteryPct,
        String qrCode
) {}