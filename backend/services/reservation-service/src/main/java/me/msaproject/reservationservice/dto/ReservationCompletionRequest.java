package me.msaproject.reservationservice.dto;

public record ReservationCompletionRequest(
        double kilometers,
        double paymentAmount,
        double lat,
        double lon,
        int batteryPct
) {}
