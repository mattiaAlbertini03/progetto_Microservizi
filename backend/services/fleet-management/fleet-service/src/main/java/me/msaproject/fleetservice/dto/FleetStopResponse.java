package me.msaproject.fleetservice.dto;

public record FleetStopResponse(
        String reservationId,
        String vehicleId,
        int minutesUsed,
        double kilometers,
        double batteryPct,
        double minimumBatteryPct
) {}
