package me.msaproject.acmeworkerservice.vehicle;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class VehicleService {

    private final VehicleRepository repository;

    @Inject
    public VehicleService(VehicleRepository repository) {
        this.repository = repository;
    }

    public List<Vehicle> getVehicles() {
        return repository.findAll();
    }

    public Optional<Vehicle> getVehicleById(String id) {
        return repository.findById(id);
    }

    public Optional<Vehicle> getVehicleByQrCode(String qrCode) {
        return repository.findByQrCode(qrCode);
    }

    public List<Vehicle> getVehiclesByStation(String stationId) {
        return repository.findByStationId(stationId);
    }
}