package me.msaproject.logisticsservice.dto.logistics;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VehicleReturnRequest(
        String stationId,
        double lat,
        double lon,
        int batteryPct
) {}
