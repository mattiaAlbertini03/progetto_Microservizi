package me.msaproject.fleetservice.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import me.msaproject.fleetservice.dto.Station;
import me.msaproject.fleetservice.dto.StationsResponse;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.List;

@ApplicationScoped
public class StationClient {

    private final Client client = ClientBuilder.newClient();
    private final String stationServiceUrl;

    @Inject
    public StationClient(
            @ConfigProperty(
                    name = "station-service.url",
                    defaultValue = "http://stations-service.acme:8000"
            )
            String stationServiceUrl
    ) {
        this.stationServiceUrl = stationServiceUrl;
    }

    public List<Station> getStations() {
        StationsResponse response = client
                .target(stationServiceUrl)
                .path("stations")
                .request()
                .get(StationsResponse.class);

        return response.stations();
    }
}