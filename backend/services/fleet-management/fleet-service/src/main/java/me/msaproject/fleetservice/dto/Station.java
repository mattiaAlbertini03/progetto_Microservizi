package me.msaproject.fleetservice.dto;

public record Station(
        String id,
        String name,
        double lat,
        double lon
) {}
