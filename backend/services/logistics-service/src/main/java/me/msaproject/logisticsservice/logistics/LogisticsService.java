package me.msaproject.logisticsservice.logistics;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import me.msaproject.logisticsservice.client.StationsClient;
import me.msaproject.logisticsservice.client.VehicleEventClient;
import me.msaproject.logisticsservice.db.VehicleRepository;
import me.msaproject.logisticsservice.dto.logistics.VehicleResponse;
import me.msaproject.logisticsservice.dto.logistics.VehicleStatus;
import me.msaproject.logisticsservice.dto.logistics.VehicleStatusResponse;
import me.msaproject.logisticsservice.dto.logistics.VehicleUpdateStatusRequest;
import me.msaproject.logisticsservice.dto.logistics.VehicleReturnRequest;

import java.util.List;

@ApplicationScoped
public class LogisticsService {

    private static final int MIN_RENTABLE_BATTERY_PCT = 80;
    private static final System.Logger LOGGER = System.getLogger(LogisticsService.class.getName());

    private final VehicleRepository vehicleRepository;

    @Inject
    public LogisticsService(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    @Inject
    VehicleEventClient vehicleEventClient;

    @Inject
    StationsClient stationsClient;

    public List<VehicleResponse> getVehicles() {
        return vehicleRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private boolean canBeRented(Vehicle vehicle) {
        return vehicle.batteryPct() >= MIN_RENTABLE_BATTERY_PCT
                && (vehicle.status() == VehicleStatus.AVAILABLE
                        || vehicle.status() == VehicleStatus.CHARGING);
    }

    public List<VehicleResponse> getVehiclesByPosition(double lat, double lon) {
        return vehicleRepository.findAll()
                .stream()
                .filter(vehicle -> isAtPosition(vehicle, lat, lon))
                .map(this::toResponse)
                .toList();
    }

    private boolean isAtPosition(Vehicle vehicle, double lat, double lon) {
        if (vehicle.lat() == null || vehicle.lon() == null) {
            return false;
        }

        double tolerance = 0.0001;

        return Math.abs(vehicle.lat() - lat) <= tolerance
                && Math.abs(vehicle.lon() - lon) <= tolerance;
    }

    public VehicleResponse getVehicle(String vehicleId) {
        return toResponse(findVehicleOrThrow(vehicleId));
    }

    public VehicleStatusResponse holdVehicle(String vehicleId) {
        Vehicle vehicle = findVehicleOrThrow(vehicleId);

        // Camunda may retry after losing an HTTP response. A completed hold is
        // therefore a successful no-op instead of a false availability error.
        if (vehicle.status() == VehicleStatus.ON_HOLD) {
            return toStatusResponse(vehicle);
        }

        if (!canBeRented(vehicle)) {
            throw new IllegalStateException(
                    "Vehicle is not available: " + vehicleId);
        }

        Vehicle updated = updateStatus(vehicle, VehicleStatus.ON_HOLD);

        return toStatusResponse(updated);
    }

    public VehicleStatusResponse pickupVehicle(String vehicleId) {
        Vehicle vehicle = findVehicleOrThrow(vehicleId);

        if (vehicle.status() != VehicleStatus.ON_HOLD) {
            throw new IllegalStateException(
                    "Vehicle cannot be picked up from status: " + vehicle.status());
        }

        Vehicle updated = updateStatus(vehicle, VehicleStatus.IN_USE);

        return toStatusResponse(updated);
    }

    public VehicleStatusResponse releaseVehicle(String vehicleId) {
        Vehicle vehicle = findVehicleOrThrow(vehicleId);

        if (vehicle.status() != VehicleStatus.ON_HOLD) {
            throw new IllegalStateException(
                    "Vehicle cannot be released from status: " + vehicle.status());
        }

        Vehicle updated = updateStatus(vehicle, VehicleStatus.AVAILABLE);

        return toStatusResponse(updated);
    }

    public VehicleStatusResponse returnVehicle(String vehicleId, VehicleReturnRequest request) {
        Vehicle vehicle = findVehicleOrThrow(vehicleId);

        if (vehicle.status() != VehicleStatus.IN_USE) {
            throw new IllegalStateException(
                    "Vehicle cannot be returned from status: " + vehicle.status());
        }

        // Fleet already resolved and validated the destination station before the
        // return workflow reached this service. Use that stable identity instead
        // of re-inferring it from an eventually consistent tracking position.
        var station = stationsClient.findStationById(request.stationId())
                .or(() -> stationsClient.findStationNear(request.lat(), request.lon()))
                .orElseThrow(() -> new IllegalStateException("Return station not found"));
        Vehicle updated = new Vehicle(
                vehicle.id(), vehicle.name(), vehicle.type(), station.lat(), station.lon(),
                VehicleStatus.AVAILABLE,
                Math.max(0, Math.min(100, request.batteryPct())), vehicle.qrCode());
        vehicleRepository.save(updated);
        vehicleEventClient.notifyVehicleUpdated(station.id(), updated.id());

        return toStatusResponse(updated);
    }

    public VehicleStatusResponse startCharging(String vehicleId) {
        Vehicle vehicle = findVehicleOrThrow(vehicleId);

        if (vehicle.status() == VehicleStatus.IN_USE ||
                vehicle.status() == VehicleStatus.ON_HOLD) {
            throw new IllegalStateException(
                    "Vehicle cannot be charged while status is: " + vehicle.status());
        }

        Vehicle updated = updateStatus(vehicle, VehicleStatus.CHARGING);

        return toStatusResponse(updated);
    }

    public VehicleStatusResponse stopCharging(String vehicleId) {
        Vehicle vehicle = findVehicleOrThrow(vehicleId);

        if (vehicle.status() != VehicleStatus.CHARGING) {
            throw new IllegalStateException(
                    "Vehicle is not charging: " + vehicleId);
        }

        Vehicle updated = updateStatus(vehicle, VehicleStatus.AVAILABLE);

        return toStatusResponse(updated);
    }

    public void increaseBattery(String vehicleId, int amount) {
        Vehicle vehicle = findVehicleOrThrow(vehicleId);

        int newBatteryPct = Math.min(100, vehicle.batteryPct() + amount);

        Vehicle updated = new Vehicle(
                vehicle.id(),
                vehicle.name(),
                vehicle.type(),
                vehicle.lat(),
                vehicle.lon(),
                vehicle.status(),
                newBatteryPct,
                vehicle.qrCode());

        vehicleRepository.save(updated);

        stationsClient.findStationIdByPosition(updated.lat(), updated.lon())
                .ifPresent(stationId -> vehicleEventClient.notifyVehicleUpdated(stationId, updated.id()));
    }

    public List<Vehicle> getVehiclesNeedingCharge() {
        return vehicleRepository.findAll()
                .stream()
                .filter(vehicle -> vehicle.batteryPct() < MIN_RENTABLE_BATTERY_PCT)
                .filter(vehicle -> vehicle.status() == VehicleStatus.AVAILABLE)
                .toList();
    }

    public List<Vehicle> getChargingVehicles() {
        return vehicleRepository.findAll()
                .stream()
                .filter(vehicle -> vehicle.status() == VehicleStatus.CHARGING)
                .toList();
    }

    private Vehicle findVehicleOrThrow(String vehicleId) {
        return vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Vehicle not found: " + vehicleId));
    }

    private Vehicle updateStatus(Vehicle vehicle, VehicleStatus status) {
        Vehicle updated = new Vehicle(
                vehicle.id(),
                vehicle.name(),
                vehicle.type(),
                vehicle.lat(),
                vehicle.lon(),
                status,
                vehicle.batteryPct(),
                vehicle.qrCode());

        vehicleRepository.save(updated);

        notifyStationBestEffort(updated);

        return updated;
    }

    private void notifyStationBestEffort(Vehicle vehicle) {
        try {
            stationsClient.findStationIdByPosition(vehicle.lat(), vehicle.lon())
                    .ifPresent(stationId -> vehicleEventClient.notifyVehicleUpdated(stationId, vehicle.id()));
        } catch (RuntimeException exception) {
            LOGGER.log(System.Logger.Level.WARNING,
                    "Vehicle state persisted but station notification failed for " + vehicle.id(),
                    exception);
        }
    }

    private VehicleResponse toResponse(Vehicle vehicle) {
        return new VehicleResponse(
                vehicle.id(),
                vehicle.name(),
                vehicle.type(),
                vehicle.lat(),
                vehicle.lon(),
                vehicle.status(),
                vehicle.batteryPct(),
                vehicle.qrCode());
    }

    private VehicleStatusResponse toStatusResponse(Vehicle vehicle) {
        return new VehicleStatusResponse(
                vehicle.id(),
                vehicle.status(),
                vehicle.batteryPct());
    }

    public Object updateVehicleStatus(String vehicleId, VehicleUpdateStatusRequest request) {
        Vehicle vehicle = findVehicleOrThrow(vehicleId);

        Vehicle updated = new Vehicle(
                vehicle.id(),
                vehicle.name(),
                vehicle.type(),
                request.lat(),
                request.lon(),
                vehicle.status(),
                request.batteryPct(),
                vehicle.qrCode());

        vehicleRepository.save(updated);

        notifyStationBestEffort(updated);

        return toResponse(updated);
    }
}
