package me.msaproject.logisticsservice.db;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import me.msaproject.logisticsservice.logistics.Vehicle;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Alternative
@Priority(1)
@ApplicationScoped
public class InMemoryVehicleRepository implements VehicleRepository {

    private final Map<String, Vehicle> vehicles = new ConcurrentHashMap<>();

    public InMemoryVehicleRepository() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("vehicles.json")) {
            if (input == null) {
                throw new IllegalStateException("vehicles.json not found");
            }
            List<Vehicle> seeded = new ObjectMapper().readValue(
                    input, new TypeReference<List<Vehicle>>() { });
            seeded.forEach(vehicle -> vehicles.put(vehicle.id(), vehicle));
        } catch (Exception exception) {
            throw new RuntimeException("Failed to seed in-memory vehicles", exception);
        }
    }

    @Override
    public List<Vehicle> findAll() {
        return List.copyOf(vehicles.values());
    }

    @Override
    public Optional<Vehicle> findById(String vehicleId) {
        return Optional.ofNullable(vehicles.get(vehicleId));
    }

    @Override
    public void save(Vehicle vehicle) {
        vehicles.put(vehicle.id(), vehicle);
    }
}
