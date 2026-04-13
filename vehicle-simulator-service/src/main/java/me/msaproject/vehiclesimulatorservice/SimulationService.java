package me.msaproject.vehiclesimulatorservice;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.*;

@ApplicationScoped
public class SimulationService {

    private static final long TICK_SECONDS = 5;
    private static final double BATTERY_CONSUMPTION_PER_KM = 1.0;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(8);

    private final Map<String, SimulationState> simulations = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();

    @Inject
    MonitoringClient monitoringClient;

    @Inject
    GraphHopperClient graphHopperClient;

    public void start(StartSimulationRequest request) {
        System.out.println("Start called for " + request.vehicleId());
        stop(request.vehicleId());

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
                routeData.distanceMeters());

        simulations.put(request.vehicleId(), state);

        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(
                () -> tick(state),
                0,
                TICK_SECONDS,
                TimeUnit.SECONDS);

        System.out.println("Task scheduled for " + request.vehicleId());
        tasks.put(request.vehicleId(), future);
    }

    public void stop(String vehicleId) {

        ScheduledFuture<?> future = tasks.remove(vehicleId);

        if (future != null) {
            future.cancel(true);
        }

        simulations.remove(vehicleId);
        System.out.println("Task ended for " + vehicleId);
    }

    public boolean isActive(String vehicleId) {
        return simulations.containsKey(vehicleId);
    }

    private void tick(SimulationState state) {
        try {
            System.out.println("Tick started for " + state.vehicleId());

            if (state.arrived()) {
                System.out.println("Vehicle already arrived: " + state.vehicleId());
                return;
            }

            long now = Instant.now().toEpochMilli();
            double elapsedSeconds = (now - state.startedAt().toEpochMilli()) / 1000.0;

            double travelledMeters = elapsedSeconds * state.speedMetersPerSecond();
            double progress = Math.min(travelledMeters / state.totalDistanceMeters(), 1.0);

            RoutePoint position = interpolateAlongRoute(state.route(), progress);

            double travelledKm = Math.min(travelledMeters, state.totalDistanceMeters()) / 1000.0;
            double battery = Math.max(
                    0,
                    state.initialBatteryPct() - travelledKm * BATTERY_CONSUMPTION_PER_KM);

            System.out.println("About to send tracking for " + state.vehicleId()
                    + " lat=" + position.lat() + " lon=" + position.lon());
            System.out.println("About to send battery for " + state.vehicleId() + " battery=" + battery);

            VehicleTrackingEvent tracking = new VehicleTrackingEvent(
                    state.vehicleId(),
                    position.lat(),
                    position.lon(),
                    now);

            VehicleBatteryEvent batteryEvent = new VehicleBatteryEvent(
                    state.vehicleId(),
                    battery,
                    now);

            monitoringClient.sendTracking(tracking);
            monitoringClient.sendBattery(batteryEvent);

            System.out.println("Events sent for " + state.vehicleId());

            if (progress >= 1.0) {
                state.setArrived(true);
                System.out.println("Vehicle arrived: " + state.vehicleId());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private RoutePoint interpolateAlongRoute(java.util.List<RoutePoint> route, double progress) {
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

    private double totalRouteDistance(java.util.List<RoutePoint> route) {
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

        double R = 6371000;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                        * Math.cos(Math.toRadians(lat2))
                        * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    @PreDestroy
    void shutdown() {

        tasks.values().forEach(t -> t.cancel(true));
        scheduler.shutdownNow();
    }
}