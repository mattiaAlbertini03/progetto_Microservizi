package me.msaproject.reservationservice.dto;

import java.math.BigDecimal;

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
        BigDecimal paymentAmount,
        Long completedAtEpochMs
) {}
