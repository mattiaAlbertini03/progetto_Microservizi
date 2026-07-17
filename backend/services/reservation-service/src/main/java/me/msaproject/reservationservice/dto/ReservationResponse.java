package me.msaproject.reservationservice.dto;

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
