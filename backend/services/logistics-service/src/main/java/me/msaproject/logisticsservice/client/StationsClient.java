package me.msaproject.logisticsservice.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import me.msaproject.logisticsservice.dto.station.StationResponse;
import me.msaproject.logisticsservice.dto.station.StationsResponse;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Optional;

@ApplicationScoped
public class StationsClient {

    private final Client client = ClientBuilder.newClient();
    private final String stationsServiceUrl;

    @Inject
    public StationsClient(
            @ConfigProperty(
                    name = "stations-service.url",
                    defaultValue = "http://stations-service.acme:8000"
            )
            String stationsServiceUrl
    ) {
        this.stationsServiceUrl = stationsServiceUrl;
    }

    public StationsResponse getStations() {
        return client
                .target(stationsServiceUrl)
                .path("stations")
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