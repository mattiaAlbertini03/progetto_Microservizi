package me.msaproject.acmemobilityservice.station;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class JsonStationRepository implements StationRepository {

    private final List<Station> stations;

    public JsonStationRepository() {
        try {
            ObjectMapper mapper = new ObjectMapper();

            InputStream inputStream = getClass()
                    .getClassLoader()
                    .getResourceAsStream("stations.json");

            if (inputStream == null) {
                throw new IllegalStateException("stations.json not found in resources");
            }

            this.stations = mapper.readValue(
                    inputStream,
                    new TypeReference<List<Station>>() {}
            );

        } catch (Exception e) {
            throw new RuntimeException("Failed to load stations.json", e);
        }
    }

    @Override
    public List<Station> findAll() {
        return stations;
    }

    @Override
    public Optional<Station> findById(String id) {
        return stations.stream()
                .filter(station -> station.id().equals(id))
                .findFirst();
    }
}