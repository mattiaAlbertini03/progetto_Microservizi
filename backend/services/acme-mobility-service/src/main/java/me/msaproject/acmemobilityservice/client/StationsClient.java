package me.msaproject.acmemobilityservice.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import me.msaproject.acmemobilityservice.dto.station.StationResponse;
import me.msaproject.acmemobilityservice.dto.station.StationsResponse;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.List;
import java.util.Map;

@ApplicationScoped
public class StationsClient {

    private final Client client = ClientBuilder.newClient();
    private final String stationServiceUrl;

    @Inject
    public StationsClient(
            @ConfigProperty(
                    name = "station-service.url",
                    defaultValue = "http://stations-service:8000"
            )
            String stationServiceUrl
    ) {
        this.stationServiceUrl = stationServiceUrl;
    }

    public List<StationResponse> getStations() {
        StationsResponse response = client
                .target(stationServiceUrl)
                .path("listStations")
                .request()
                .post(Entity.json(Map.of()), StationsResponse.class);

        return response.stations();
    }

    public StationResponse getStation(String stationId) {
        return client
                .target(stationServiceUrl)
                .path("getStation")
                .request()
                .post(Entity.json(Map.of("id", stationId)), StationResponse.class);
    }
}
