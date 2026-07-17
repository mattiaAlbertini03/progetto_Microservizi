package me.msaproject.acmemobilityservice.dto.station;

public record StationResponse(
        String id,
        String name,
        double lat,
        double lon
) {}
