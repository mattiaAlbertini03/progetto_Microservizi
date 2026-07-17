package me.msaproject.acmemobilityservice.dto.reservation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ReservationResponse(
        String reservationId,
        String userId,
        String vehicleId,
        String stationId,
        String status,
        long createdAtEpochMs,
        long scheduledStartEpochMs,
        long pickupDeadlineEpochMs,
        Double totalKilometers,
        Double paymentAmount,
        Long completedAtEpochMs
) {}
