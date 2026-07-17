package me.msaproject.simulatorservice.simulation;

import java.time.Instant;
import java.util.List;

import me.msaproject.simulatorservice.dto.RoutePoint;

public class SimulationState {

    private final String vehicleId;
    private final double speedMetersPerSecond;
    private final double initialBatteryPct;
    private final Instant startedAt;
    private final List<RoutePoint> route;
    private final double totalDistanceMeters;
    private final double targetBatteryPct;
    private final double batteryDrainPerMeter;

    private volatile boolean arrived;
    private volatile double travelledMeters;
    private volatile double currentBatteryPct;
    private volatile double minimumBatteryPct;
    private volatile long lastTickEpochMs;
    private volatile long rechargeUntilEpochMs;
    private volatile boolean safetyRechargeTriggered;

    public SimulationState(
            String vehicleId,
            double speedMetersPerSecond,
            double initialBatteryPct,
            Instant startedAt,
            List<RoutePoint> route,
            double totalDistanceMeters,
            double targetBatteryPct
    ) {
        this.vehicleId = vehicleId;
        this.speedMetersPerSecond = speedMetersPerSecond;
        this.initialBatteryPct = initialBatteryPct;
        this.startedAt = startedAt;
        this.route = route;
        this.totalDistanceMeters = totalDistanceMeters;
        this.targetBatteryPct = targetBatteryPct;
        this.batteryDrainPerMeter = totalDistanceMeters <= 0 ? 0
                : (initialBatteryPct - targetBatteryPct) / totalDistanceMeters;
        this.arrived = false;
        this.currentBatteryPct = initialBatteryPct;
        this.minimumBatteryPct = initialBatteryPct;
        this.lastTickEpochMs = startedAt.toEpochMilli();
    }

    public String vehicleId() { return vehicleId; }
    public double speedMetersPerSecond() { return speedMetersPerSecond; }
    public double initialBatteryPct() { return initialBatteryPct; }
    public Instant startedAt() { return startedAt; }
    public List<RoutePoint> route() { return route; }
    public double totalDistanceMeters() { return totalDistanceMeters; }
    public double targetBatteryPct() { return targetBatteryPct; }
    public double batteryDrainPerMeter() { return batteryDrainPerMeter; }
    public double travelledMeters() { return travelledMeters; }
    public void setTravelledMeters(double value) { travelledMeters = value; }
    public double currentBatteryPct() { return currentBatteryPct; }
    public void setCurrentBatteryPct(double value) { currentBatteryPct = value; minimumBatteryPct = Math.min(minimumBatteryPct, value); }
    public double minimumBatteryPct() { return minimumBatteryPct; }
    public long lastTickEpochMs() { return lastTickEpochMs; }
    public void setLastTickEpochMs(long value) { lastTickEpochMs = value; }
    public long rechargeUntilEpochMs() { return rechargeUntilEpochMs; }
    public void setRechargeUntilEpochMs(long value) { rechargeUntilEpochMs = value; }
    public boolean safetyRechargeTriggered() { return safetyRechargeTriggered; }
    public void setSafetyRechargeTriggered(boolean value) { safetyRechargeTriggered = value; }

    public boolean arrived() { return arrived; }
    public void setArrived(boolean arrived) { this.arrived = arrived; }
}
