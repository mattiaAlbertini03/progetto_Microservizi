package me.msaproject.logisticsservice.db;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import me.msaproject.logisticsservice.logistics.Vehicle;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// @ApplicationScoped
public class JsonVehicleRepository implements VehicleRepository {

    private final Map<String, Vehicle> vehicles;

    public JsonVehicleRepository() {
        try {
            ObjectMapper mapper = new ObjectMapper();

            InputStream inputStream = getClass()
                    .getClassLoader()
                    .getResourceAsStream("vehicles.json");

            if (inputStream == null) {
                throw new IllegalStateException(
                        "vehicles.json not found in resources"
                );
            }

            List<Vehicle> loadedVehicles = mapper.readValue(
                    inputStream,
                    new TypeReference<List<Vehicle>>() {}
            );

            this.vehicles = new ConcurrentHashMap<>();

            for (Vehicle vehicle : loadedVehicles) {
                vehicles.put(vehicle.id(), vehicle);
            }

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to load vehicles.json",
                    e
            );
        }
    }

    @Override
    public List<Vehicle> findAll() {
        return List.copyOf(vehicles.values());
    }

    @Override
    public java.util.Optional<Vehicle> findById(String vehicleId) {
        return java.util.Optional.ofNullable(vehicles.get(vehicleId));
    }

    @Override
    public void save(Vehicle vehicle) {
        vehicles.put(vehicle.id(), vehicle);
    }
}
