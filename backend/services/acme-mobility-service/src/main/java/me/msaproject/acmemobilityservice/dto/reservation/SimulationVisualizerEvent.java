package me.msaproject.acmemobilityservice.dto.reservation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SimulationVisualizerEvent(
        String reservationId,
        double amount,
        double distance,
        String vehicleName,
        String username
) {
}
