package me.msaproject.logisticsservice.logistics;
import me.msaproject.logisticsservice.dto.logistics.VehicleStatus;

public record Vehicle(
        String id,
        String type,
        Double lat,
        Double lon,
        VehicleStatus status,
        int batteryPct,
        String qrCode
) {}