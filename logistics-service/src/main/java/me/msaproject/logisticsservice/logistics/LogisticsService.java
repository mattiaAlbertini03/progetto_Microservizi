package me.msaproject.logisticsservice.logistics;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import me.msaproject.logisticsservice.db.VehicleRepository;
import me.msaproject.logisticsservice.dto.VehicleResponse;
import me.msaproject.logisticsservice.dto.VehicleStatus;
import me.msaproject.logisticsservice.dto.VehicleStatusResponse;

import java.util.List;

@ApplicationScoped
public class LogisticsService {

    private final VehicleRepository vehicleRepository;

    @Inject
    public LogisticsService(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    public List<VehicleResponse> getVehicles() {
        return vehicleRepository.getVehicles()
                .values()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public VehicleResponse getVehicle(String vehicleId) {
        return toResponse(findVehicleOrThrow(vehicleId));
    }

    public VehicleStatusResponse holdVehicle(String vehicleId) {
        Vehicle vehicle = findVehicleOrThrow(vehicleId);

        if (vehicle.status() != VehicleStatus.AVAILABLE) {
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

        Vehicle updated = updateStatus(vehicle, VehicleStatus.AVAILABLE);

        return toStatusResponse(updated);
    }

    public VehicleStatusResponse returnVehicle(String vehicleId) {
        Vehicle vehicle = findVehicleOrThrow(vehicleId);

        if (vehicle.status() == VehicleStatus.IN_USE) {
            throw new IllegalStateException(
                    "Vehicle cannot be returned from status: " + vehicle.status());
        }

        Vehicle updated = updateStatus(vehicle, VehicleStatus.AVAILABLE);

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
                vehicle.type(),
                vehicle.lat(),
                vehicle.lon(),
                vehicle.status(),
                newBatteryPct,
                vehicle.qrCode());

        vehicleRepository.getVehicles().put(vehicleId, updated);
        vehicleRepository.sync();
    }

    public List<Vehicle> getVehiclesNeedingCharge() {
        return vehicleRepository.getVehicles()
                .values()
                .stream()
                .filter(vehicle -> vehicle.batteryPct() < 20)
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
                vehicle.type(),
                vehicle.lat(),
                vehicle.lon(),
                status,
                vehicle.batteryPct(),
                vehicle.qrCode());

        vehicleRepository.getVehicles().put(vehicle.id(), updated);
        vehicleRepository.sync();

        return updated;
    }

    private VehicleResponse toResponse(Vehicle vehicle) {
        return new VehicleResponse(
                vehicle.id(),
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