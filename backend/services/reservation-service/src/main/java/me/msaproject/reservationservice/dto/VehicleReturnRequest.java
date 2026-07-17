package me.msaproject.reservationservice.dto;

public record VehicleReturnRequest(
        String stationId,
        double lat,
        double lon,
        int batteryPct
) {}
