package me.msaproject.logisticsservice.db;

import java.util.List;
import java.util.Optional;

import me.msaproject.logisticsservice.logistics.Vehicle;

public interface VehicleRepository {

    List<Vehicle> findAll();

    Optional<Vehicle> findById(String vehicleId);

    void save(Vehicle vehicle);
}
