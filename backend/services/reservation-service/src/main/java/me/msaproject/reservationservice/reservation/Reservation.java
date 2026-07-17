package me.msaproject.reservationservice.reservation;

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
        Double paymentAmount,
        Long completedAtEpochMs
) {}
