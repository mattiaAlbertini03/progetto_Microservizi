package me.msaproject.fleetservice.dto;

public record Vehicle(
        String id,
        String type,
        Double lat,
        Double lon,
        String status,
        int batteryPct,
        String qrCode
) {}