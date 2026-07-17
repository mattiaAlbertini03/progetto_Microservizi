package me.msaproject.logisticsservice.dto.logistics;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VehicleReturnRequest(double lat, double lon, int batteryPct) {}
