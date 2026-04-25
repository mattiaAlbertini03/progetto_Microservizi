package me.msaproject.acmeworkerservice.vehicle;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class JsonVehicleRepository implements VehicleRepository {

    private final List<Vehicle> vehicles;

    public JsonVehicleRepository() {
        try {
            ObjectMapper mapper = new ObjectMapper();

            InputStream inputStream = getClass()
                    .getClassLoader()
                    .getResourceAsStream("vehicles.json");

            if (inputStream == null) {
                throw new IllegalStateException("vehicles.json not found in resources");
            }

            this.vehicles = mapper.readValue(
                    inputStream,
                    new TypeReference<List<Vehicle>>() {
                    });
        } catch (Exception e) {
            throw new RuntimeException("Failed to load vehicles.json", e);
        }
    }

    @Override
    public List<Vehicle> findAll() {
        return vehicles;
    }

    @Override
    public Optional<Vehicle> findById(String id) {
        return vehicles.stream()
                .filter(vehicle -> vehicle.id().equals(id))
                .findFirst();
    }

    @Override
    public Optional<Vehicle> findByQrCode(String qrCode) {
        return vehicles.stream()
                .filter(vehicle -> vehicle.qrCode().equals(qrCode))
                .findFirst();
    }

    @Override
    public List<Vehicle> findByStationId(String stationId) {
        return vehicles.stream()
                .filter(vehicle -> vehicle.stationId().equals(stationId))
                .toList();
    }

    @Override
    public void updateStatus(String vehicleId, String status) {
        for (int i = 0; i < vehicles.size(); i++) {
            Vehicle v = vehicles.get(i);

            if (v.id().equals(vehicleId)) {
                vehicles.set(i, new Vehicle(
                        v.id(),
                        v.type(),
                        v.stationId(),
                        status,
                        v.batteryPct(),
                        v.qrCode()));
                return;
            }
        }

        throw new IllegalArgumentException("Vehicle not found: " + vehicleId);
    }
}