package me.msaproject.queryservice;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class VehicleStore {
    private final java.util.concurrent.ConcurrentHashMap<String, VehicleSnapshot> map = new java.util.concurrent.ConcurrentHashMap<>();

    public VehicleSnapshot get(String vehicleId) { return map.get(vehicleId); }

    public java.util.Collection<VehicleSnapshot> all() { return map.values(); }

    public void upsertTracking(String vehicleId, double lat, double lon, long ts) {
        map.compute(vehicleId, (k, old) -> new VehicleSnapshot(
            vehicleId,
            lat,
            lon,
            ts,
            old != null ? old.batteryPercent() : null,
            old != null ? old.batteryTimestampEpochMs() : null
        ));
    }

    public void upsertBattery(String vehicleId, Double batteryPercent, long ts) {
        map.compute(vehicleId, (k, old) -> new VehicleSnapshot(
            vehicleId,
            old != null ? old.lat() : null,
            old != null ? old.lon() : null,
            old != null ? old.trackingTimestampEpochMs() : null,
            batteryPercent,
            ts
        ));
    }
}
