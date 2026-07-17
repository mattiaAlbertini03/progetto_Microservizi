package me.msaproject.fleetservice.dto;

public record VehicleAtStationResponse(
        boolean atStation,
        boolean stopped,
        String movementStatus,
        String stationId
) {
}
