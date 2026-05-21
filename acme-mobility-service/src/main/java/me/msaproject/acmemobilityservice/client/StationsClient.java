package me.msaproject.acmemobilityservice.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import me.msaproject.acmemobilityservice.dto.station.StationResponse;

import java.util.Arrays;
import java.util.List;

@ApplicationScoped
public class StationsClient {

    private static final String STATION_BASE_URL =
            "http://stations-service.acme:8080";

    private final Client client = ClientBuilder.newClient();

    public List<StationResponse> getStations() {
        StationResponse[] stations = client
                .target(STATION_BASE_URL)
                .path("/stations")
                .request()
                .get(StationResponse[].class);

        return Arrays.asList(stations);
    }
}