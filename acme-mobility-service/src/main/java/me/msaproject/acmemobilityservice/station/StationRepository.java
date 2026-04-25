package me.msaproject.acmeworkerservice.station;

import java.util.List;
import java.util.Optional;

public interface StationRepository {
    List<Station> findAll();
    Optional<Station> findById(String id);
}