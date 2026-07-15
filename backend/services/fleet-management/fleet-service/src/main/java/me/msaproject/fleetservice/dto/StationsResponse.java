package me.msaproject.fleetservice.dto;

import java.util.List;

public record StationsResponse(
        List<Station> stations
) {}