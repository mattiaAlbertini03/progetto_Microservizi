package me.msaproject.logisticsservice.logistics;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import me.msaproject.logisticsservice.client.StationsClient;
import me.msaproject.logisticsservice.client.VehicleEventClient;
import me.msaproject.logisticsservice.db.VehicleRepository;
import me.msaproject.logisticsservice.dto.logistics.VehicleResponse;
import me.msaproject.logisticsservice.dto.logistics.VehicleStatus;
import me.msaproject.logisticsservice.dto.logistics.VehicleStatusResponse;
import me.msaproject.logisticsservice.dto.logistics.VehicleReturnRequest;

import java.util.List;

@ApplicationScoped
public class LogisticsService {

    private static final int MIN_RENTABLE_BATTERY_PCT = 80;

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
        return vehicleRepository.getVehicles()
                .values()
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
        return vehicleRepository.getVehicles()
                .values()
                .stream()
                .filter(vehicle -> canBeRented(vehicle))
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

        var station = stationsClient.findStationNear(request.lat(), request.lon())
                .orElseThrow(() -> new IllegalStateException("Vehicle is not near a station"));
        Vehicle updated = new Vehicle(
                vehicle.id(), vehicle.name(), vehicle.type(), station.lat(), station.lon(),
                VehicleStatus.AVAILABLE,
                Math.max(0, Math.min(100, request.batteryPct())), vehicle.qrCode());
        vehicleRepository.getVehicles().put(vehicle.id(), updated);
        vehicleRepository.sync();
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

        vehicleRepository.getVehicles().put(vehicleId, updated);
        vehicleRepository.sync();

        stationsClient.findStationIdByPosition(updated.lat(), updated.lon())
                .ifPresent(stationId -> vehicleEventClient.notifyVehicleUpdated(stationId, updated.id()));
    }

    public List<Vehicle> getVehiclesNeedingCharge() {
        return vehicleRepository.getVehicles()
                .values()
                .stream()
                .filter(vehicle -> vehicle.batteryPct() < 100)
                .filter(vehicle -> vehicle.status() == VehicleStatus.AVAILABLE)
                .toList();
    }

    public List<Vehicle> getChargingVehicles() {
        return vehicleRepository.getVehicles()
                .values()
                .stream()
                .filter(vehicle -> vehicle.status() == VehicleStatus.CHARGING)
                .toList();
    }

    private Vehicle findVehicleOrThrow(String vehicleId) {
        Vehicle vehicle = vehicleRepository.getVehicles().get(vehicleId);

        if (vehicle == null) {
            throw new IllegalArgumentException("Vehicle not found: " + vehicleId);
        }

        return vehicle;
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

        vehicleRepository.getVehicles().put(vehicle.id(), updated);
        vehicleRepository.sync();

        stationsClient.findStationIdByPosition(updated.lat(), updated.lon())
                .ifPresent(stationId -> vehicleEventClient.notifyVehicleUpdated(stationId, updated.id()));

        return updated;
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
}
