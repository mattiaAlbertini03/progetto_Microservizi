package me.msaproject.logisticsservice.dto.station;

public record StationResponse(
        String id,
        String name,
        double lat,
        double lon,
        int capacity
) {}