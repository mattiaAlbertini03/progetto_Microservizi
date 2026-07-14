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

    private volatile boolean arrived;

    public SimulationState(
            String vehicleId,
            double speedMetersPerSecond,
            double initialBatteryPct,
            Instant startedAt,
            List<RoutePoint> route,
            double totalDistanceMeters
    ) {
        this.vehicleId = vehicleId;
        this.speedMetersPerSecond = speedMetersPerSecond;
        this.initialBatteryPct = initialBatteryPct;
        this.startedAt = startedAt;
        this.route = route;
        this.totalDistanceMeters = totalDistanceMeters;
        this.arrived = false;
    }

    public String vehicleId() { return vehicleId; }
    public double speedMetersPerSecond() { return speedMetersPerSecond; }
    public double initialBatteryPct() { return initialBatteryPct; }
    public Instant startedAt() { return startedAt; }
    public List<RoutePoint> route() { return route; }
    public double totalDistanceMeters() { return totalDistanceMeters; }

    public boolean arrived() { return arrived; }
    public void setArrived(boolean arrived) { this.arrived = arrived; }
}