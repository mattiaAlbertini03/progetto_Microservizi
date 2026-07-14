package me.msaproject.logisticsservice.db;

import java.util.Map;

import me.msaproject.logisticsservice.logistics.Vehicle;

public interface VehicleRepository {

    Map<String, Vehicle> getVehicles();

    void sync();
}