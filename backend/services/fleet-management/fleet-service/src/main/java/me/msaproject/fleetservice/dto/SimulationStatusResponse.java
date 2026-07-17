package me.msaproject.fleetservice.dto;

public record SimulationStatusResponse(
        String vehicleId,
        boolean active,
        String movementStatus,
        double kilometers,
        int minutesUsed
) {
}
