package me.msaproject.simulatorservice.simulation;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import me.msaproject.simulatorservice.client.GraphHopperClient;
import me.msaproject.simulatorservice.client.MonitoringClient;
import me.msaproject.simulatorservice.dto.RouteData;
import me.msaproject.simulatorservice.dto.RoutePoint;
import me.msaproject.simulatorservice.dto.SimulationSummary;
import me.msaproject.simulatorservice.dto.SimulationStatusResponse;
import me.msaproject.simulatorservice.dto.StartSimulationRequest;
import me.msaproject.simulatorservice.dto.VehicleBatteryEvent;
import me.msaproject.simulatorservice.dto.VehicleTrackingEvent;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class SimulationService {

    private static final System.Logger LOGGER = System.getLogger(SimulationService.class.getName());
    private static final long TICK_SECONDS = 5;
    private static final double SAFETY_BATTERY_PCT = 5.0;
    private static final double RESUME_BATTERY_PCT = 25.0;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(8);

    private final Map<String, SimulationState> simulations = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();

    @Inject
    MonitoringClient monitoringClient;

    @Inject
    GraphHopperClient graphHopperClient;

    public void start(StartSimulationRequest request) {
        discardSimulation(request.vehicleId());

        RouteData routeData = graphHopperClient.getRoute(
                request.fromLat(),
                request.fromLon(),
                request.toLat(),
                request.toLon());

        SimulationState state = new SimulationState(
                request.vehicleId(),
                request.speedMetersPerSecond(),
                request.initialBatteryPct(),
                Instant.now(),
                routeData.points(),
                routeData.distanceMeters(),
                ThreadLocalRandom.current().nextDouble(0.0, Math.max(0.01, request.initialBatteryPct())));

        simulations.put(request.vehicleId(), state);

        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(
                () -> tick(state),
                0,
                TICK_SECONDS,
                TimeUnit.SECONDS);

        tasks.put(request.vehicleId(), future);
        LOGGER.log(System.Logger.Level.INFO, "Simulation started for vehicle {0}", request.vehicleId());
    }

    public SimulationSummary stop(String vehicleId) {

        SimulationState state = simulations.remove(vehicleId);

        ScheduledFuture<?> future = tasks.remove(vehicleId);

        if (future != null) {
            future.cancel(true);
        }

        if (state == null) {
            throw new IllegalStateException("No active simulation for vehicle " + vehicleId);
        }

        double elapsedSeconds = Math.max(
                0,
                (Instant.now().toEpochMilli() - state.startedAt().toEpochMilli()) / 1000.0);
        double travelledMeters = state.travelledMeters();
        double batteryPct = state.currentBatteryPct();

        LOGGER.log(System.Logger.Level.INFO, "Simulation stopped for vehicle {0}", vehicleId);

        return new SimulationSummary(
                vehicleId,
                Math.max(1, (int) Math.ceil(elapsedSeconds / 60.0)),
                travelledMeters / 1000.0,
                batteryPct,
                state.minimumBatteryPct());
    }

    private void discardSimulation(String vehicleId) {
        ScheduledFuture<?> future = tasks.remove(vehicleId);

        if (future != null) {
            future.cancel(true);
        }

        simulations.remove(vehicleId);
    }

    public boolean isActive(String vehicleId) {
        return simulations.containsKey(vehicleId);
    }

    public SimulationStatusResponse getStatus(String vehicleId) {
        SimulationState state = simulations.get(vehicleId);
        boolean active = state != null;
        boolean moving = active
                && !state.arrived()
                && state.rechargeUntilEpochMs() == 0;
        double kilometers = active ? state.travelledMeters() / 1000.0 : 0.0;
        int minutesUsed = active
                ? Math.max(1, (int) Math.ceil(
                        (Instant.now().toEpochMilli() - state.startedAt().toEpochMilli()) / 60_000.0))
                : 0;

        return new SimulationStatusResponse(
                vehicleId,
                active,
                moving ? "MOVING" : "STOPPED",
                kilometers,
                minutesUsed);
    }

    private synchronized void tick(SimulationState state) {
        try {
            if (state.arrived()) {
                return;
            }

            long now = Instant.now().toEpochMilli();
            if (state.rechargeUntilEpochMs() > 0) {
                publishState(state, now);
                return;
            }

            double elapsedSeconds = Math.max(0, (now - state.lastTickEpochMs()) / 1000.0);
            state.setLastTickEpochMs(now);
            double travelledMeters = Math.min(
                    state.totalDistanceMeters(),
                    state.travelledMeters() + elapsedSeconds * state.speedMetersPerSecond());
            double travelledThisTick = travelledMeters - state.travelledMeters();
            state.setTravelledMeters(travelledMeters);
            state.setCurrentBatteryPct(Math.max(0,
                    state.currentBatteryPct() - travelledThisTick * state.batteryDrainPerMeter()));
            double progress = Math.min(travelledMeters / state.totalDistanceMeters(), 1.0);

            publishState(state, now);

            if (!state.safetyRechargeTriggered()
                    && state.currentBatteryPct() <= SAFETY_BATTERY_PCT
                    && progress < 1.0) {
                state.setSafetyRechargeTriggered(true);
                state.setRechargeUntilEpochMs(Long.MAX_VALUE);
                LOGGER.log(System.Logger.Level.INFO,
                        "Vehicle {0} paused at the {1}% safety threshold",
                        state.vehicleId(), SAFETY_BATTERY_PCT);
                return;
            }

            if (progress >= 1.0) {
                state.setArrived(true);
                LOGGER.log(System.Logger.Level.INFO, "Vehicle {0} reached its destination", state.vehicleId());
            }
        } catch (Exception exception) {
            LOGGER.log(System.Logger.Level.ERROR,
                    "Simulation tick failed for vehicle " + state.vehicleId(), exception);
        }
    }

    public void rechargeAndResume(String vehicleId) {
        SimulationState state = simulations.get(vehicleId);
        if (state == null) {
            throw new IllegalStateException("No active simulation for vehicle " + vehicleId);
        }
        state.setCurrentBatteryPct(RESUME_BATTERY_PCT);
        state.setRechargeUntilEpochMs(0);
        state.setLastTickEpochMs(Instant.now().toEpochMilli());
        LOGGER.log(System.Logger.Level.INFO, "Vehicle {0} recharged and resumed", vehicleId);
    }

    private void publishState(SimulationState state, long now) {
        double progress = state.totalDistanceMeters() == 0 ? 1
                : Math.min(state.travelledMeters() / state.totalDistanceMeters(), 1.0);
        RoutePoint position = interpolateAlongRoute(state.route(), progress);
        monitoringClient.sendTracking(new VehicleTrackingEvent(
                state.vehicleId(), position.lat(), position.lon(), now));
        monitoringClient.sendBattery(new VehicleBatteryEvent(
                state.vehicleId(), state.currentBatteryPct(), now));
    }

    private RoutePoint interpolateAlongRoute(List<RoutePoint> route, double progress) {
        if (route == null || route.isEmpty()) {
            throw new IllegalStateException("Route is empty");
        }
        if (route.size() == 1) {
            return route.get(0);
        }
        if (progress <= 0.0) {
            return route.get(0);
        }
        if (progress >= 1.0) {
            return route.get(route.size() - 1);
        }

        double total = totalRouteDistance(route);
        double target = total * progress;
        double covered = 0.0;

        for (int i = 0; i < route.size() - 1; i++) {
            RoutePoint a = route.get(i);
            RoutePoint b = route.get(i + 1);

            double segment = distanceMeters(a.lat(), a.lon(), b.lat(), b.lon());

            if (covered + segment >= target) {
                double localT = (target - covered) / segment;
                double lat = lerp(a.lat(), b.lat(), localT);
                double lon = lerp(a.lon(), b.lon(), localT);
                return new RoutePoint(lat, lon);
            }

            covered += segment;
        }

        return route.get(route.size() - 1);
    }

    private double totalRouteDistance(List<RoutePoint> route) {
        double total = 0.0;
        for (int i = 0; i < route.size() - 1; i++) {
            RoutePoint a = route.get(i);
            RoutePoint b = route.get(i + 1);
            total += distanceMeters(a.lat(), a.lon(), b.lat(), b.lon());
        }
        return total;
    }

    private double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private double distanceMeters(double lat1, double lon1,
            double lat2, double lon2) {

        double earthRadiusMeters = 6_371_000;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                        * Math.cos(Math.toRadians(lat2))
                        * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return earthRadiusMeters * c;
    }

    @PreDestroy
    void shutdown() {
        tasks.values().forEach(task -> task.cancel(true));
        scheduler.shutdownNow();
    }
}
