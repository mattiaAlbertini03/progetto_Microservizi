package me.msaproject.acmemobilityservice.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import me.msaproject.acmemobilityservice.dto.station.StationResponse;
import me.msaproject.acmemobilityservice.dto.station.StationsResponse;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.List;

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
                .path("stations")
                .request()
                .get(StationsResponse.class);

        return response.stations();
    }

    public StationResponse getStation(String stationId) {
        return client
                .target(stationServiceUrl)
                .path("stations")
                .path(stationId)
                .request()
                .get(StationResponse.class);
    }
}