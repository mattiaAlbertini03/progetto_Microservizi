package me.msaproject.acmemobilityservice.dto.reservation;

public record QrPickupRequest(
                String userId,
                String vehicleId,
                String stationId,
                String qrCode) {
}
