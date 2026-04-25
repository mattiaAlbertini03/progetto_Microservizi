package me.msaproject.acmemobilityservice.station;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class StationService {

    private final StationRepository repository;

    @Inject
    public StationService(StationRepository repository) {
        this.repository = repository;
    }

    public List<Station> getStations() {
        return repository.findAll();
    }

    public Optional<Station> getStationById(String id) {
        return repository.findById(id);
    }
}