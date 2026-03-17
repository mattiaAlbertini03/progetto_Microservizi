package me.msaproject.vehiclesimulatorservice;

import java.time.Instant;

public class SimulationState {

    private final String vehicleId;

    private final double fromLat;
    private final double fromLon;

    private final double toLat;
    private final double toLon;

    private final double speedMetersPerSecond;
    private final double initialBatteryPct;

    private final Instant startedAt;

    private volatile boolean arrived;

    public SimulationState(
            String vehicleId,
            double fromLat,
            double fromLon,
            double toLat,
            double toLon,
            double speedMetersPerSecond,
            double initialBatteryPct,
            Instant startedAt
    ) {
        this.vehicleId = vehicleId;
        this.fromLat = fromLat;
        this.fromLon = fromLon;
        this.toLat = toLat;
        this.toLon = toLon;
        this.speedMetersPerSecond = speedMetersPerSecond;
        this.initialBatteryPct = initialBatteryPct;
        this.startedAt = startedAt;
        this.arrived = false;
    }

    public String vehicleId() { return vehicleId; }

    public double fromLat() { return fromLat; }
    public double fromLon() { return fromLon; }

    public double toLat() { return toLat; }
    public double toLon() { return toLon; }

    public double speedMetersPerSecond() { return speedMetersPerSecond; }

    public double initialBatteryPct() { return initialBatteryPct; }

    public Instant startedAt() { return startedAt; }

    public boolean arrived() { return arrived; }
    public void setArrived(boolean arrived) { this.arrived = arrived; }
}