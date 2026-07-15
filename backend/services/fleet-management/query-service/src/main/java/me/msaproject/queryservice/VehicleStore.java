package me.msaproject.queryservice;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@ApplicationScoped
public class VehicleStore {

    private final ConcurrentMap<String, VehicleSnapshot> snapshots =
            new ConcurrentHashMap<>();

    public VehicleSnapshot get(
            String vehicleId
    ) {
        return snapshots.get(vehicleId);
    }

    public Collection<VehicleSnapshot> all() {
        ArrayList<VehicleSnapshot> result =
                new ArrayList<>(snapshots.values());

        result.sort(
                Comparator.comparing(
                        VehicleSnapshot::vehicleId
                )
        );

        return result;
    }

    public void upsertTracking(
            String vehicleId,
            double lat,
            double lon,
            long timestampEpochMs
    ) {
        snapshots.compute(
                vehicleId,
                (key, current) ->
                        new VehicleSnapshot(
                                vehicleId,
                                lat,
                                lon,
                                timestampEpochMs,
                                current != null
                                        ? current.batteryPercent()
                                        : null,
                                current != null
                                        ? current.batteryTimestampEpochMs()
                                        : null
                        )
        );
    }

    public void upsertBattery(
            String vehicleId,
            double batteryPercent,
            long timestampEpochMs
    ) {
        snapshots.compute(
                vehicleId,
                (key, current) ->
                        new VehicleSnapshot(
                                vehicleId,
                                current != null
                                        ? current.lat()
                                        : null,
                                current != null
                                        ? current.lon()
                                        : null,
                                current != null
                                        ? current.trackingTimestampEpochMs()
                                        : null,
                                batteryPercent,
                                timestampEpochMs
                        )
        );
    }
}