package me.msaproject.acmeworkerservice.station;

public record Station(
        String id,
        String name,
        double lat,
        double lon,
        int capacity
) {}
