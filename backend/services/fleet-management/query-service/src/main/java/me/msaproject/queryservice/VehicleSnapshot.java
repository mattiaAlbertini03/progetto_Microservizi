package me.msaproject.queryservice;

public record VehicleSnapshot(
        String vehicleId,
        Double lat,
        Double lon,
        Long trackingTimestampEpochMs,
        Double batteryPercent,
        Long batteryTimestampEpochMs
) {

    public boolean hasTracking() {
        return lat != null
                && lon != null
                && trackingTimestampEpochMs != null;
    }

    public boolean hasBattery() {
        return batteryPercent != null
                && batteryTimestampEpochMs != null;
    }
}