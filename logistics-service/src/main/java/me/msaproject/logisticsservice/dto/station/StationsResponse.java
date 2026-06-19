package me.msaproject.logisticsservice.dto.station;

import java.util.List;

public record StationsResponse(
        List<StationResponse> stations
) {}