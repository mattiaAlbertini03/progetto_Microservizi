package me.msaproject.reservationservice.dto;

public record ReservationCompletionRequest(
        double kilometers,
        double paymentAmount,
        String stationId,
        double lat,
        double lon,
        int batteryPct
) {}
