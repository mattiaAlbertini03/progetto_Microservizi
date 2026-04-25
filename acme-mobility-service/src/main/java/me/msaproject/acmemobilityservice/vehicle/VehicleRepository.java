package me.msaproject.acmeworkerservice.vehicle;

import java.util.List;
import java.util.Optional;

public interface VehicleRepository {
    List<Vehicle> findAll();
    Optional<Vehicle> findById(String id);
    Optional<Vehicle> findByQrCode(String qrCode);
    List<Vehicle> findByStationId(String stationId);

    void updateStatus(String vehicleId, String status);
}