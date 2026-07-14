package me.msaproject.acmemobilityservice.dto.station;

import java.util.List;

public record StationsResponse(
        List<StationResponse> stations
) {}