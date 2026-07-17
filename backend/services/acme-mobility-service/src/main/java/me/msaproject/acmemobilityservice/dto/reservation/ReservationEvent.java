package me.msaproject.acmemobilityservice.dto.reservation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ReservationEvent(
        String reservationId,
        String status,
        String message,
        Double depositAmount,
        String currency,
        Double batteryPct,
        Integer penaltyPercentage,
        Double baseAmount,
        Double finalAmount,
        Double paymentAmount,
        String stationId,
        String movementStatus
) {
    public ReservationEvent(
            String reservationId,
            String status,
            String message) {
        this(
                reservationId,
                status,
                message,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }
}
