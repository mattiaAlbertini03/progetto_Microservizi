package me.msaproject.logisticsservice.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import me.msaproject.logisticsservice.dto.station.StationResponse;
import me.msaproject.logisticsservice.dto.station.StationsResponse;

import java.util.Optional;

@ApplicationScoped
public class StationsClient {

    private static final String STATIONS_BASE_URL =
            "http://stations-service.acme:8000";

    private final Client client = ClientBuilder.newClient();

    public StationsResponse getStations() {
        return client
                .target(STATIONS_BASE_URL)
                .path("/stations")
                .request()
                .get(StationsResponse.class);
    }

    public Optional<String> findStationIdByPosition(Double lat, Double lon) {
        if (lat == null || lon == null) {
            return Optional.empty();
        }

        double tolerance = 0.0001;

        return getStations()
                .stations()
                .stream()
                .filter(station ->
                        Math.abs(station.lat() - lat) <= tolerance
                                && Math.abs(station.lon() - lon) <= tolerance
                )
                .map(StationResponse::id)
                .findFirst();
    }
}