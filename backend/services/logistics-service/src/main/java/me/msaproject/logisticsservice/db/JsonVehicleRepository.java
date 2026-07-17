package me.msaproject.logisticsservice.db;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import me.msaproject.logisticsservice.logistics.Vehicle;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
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
    public Map<String, Vehicle> getVehicles() {
        return vehicles;
    }

    @Override
    public void sync() {
        // The JSON-backed demo repository keeps all runtime changes in memory.
        // A persistent repository can implement this hook when one is introduced.
    }
}
