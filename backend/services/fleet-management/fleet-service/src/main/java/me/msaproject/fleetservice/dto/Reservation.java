package me.msaproject.fleetservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Reservation(
        String reservationId,
        String userId,
        String vehicleId,
        String stationId,
        String status,
        long createdAtEpochMs,
        long scheduledStartEpochMs,
        long pickupDeadlineEpochMs
) {}
