package me.msaproject.reservationservice.reservation;

import java.math.BigDecimal;

public record Reservation(
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
