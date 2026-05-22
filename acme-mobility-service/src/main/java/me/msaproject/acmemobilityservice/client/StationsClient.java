package me.msaproject.acmemobilityservice.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import me.msaproject.acmemobilityservice.dto.station.StationResponse;
import me.msaproject.acmemobilityservice.dto.station.StationsResponse;

import java.util.List;

@ApplicationScoped
public class StationsClient {

    private static final String STATION_BASE_URL = "http://stations-service:8000";

    private final Client client = ClientBuilder.newClient();

    public List<StationResponse> getStations() {
        StationsResponse response = client
                .target(STATION_BASE_URL)
                .path("/stations")
                .request()
                .get(StationsResponse.class);

        return response.stations();
    }

    public StationResponse getStation(String stationId) {
        return client
                .target(STATION_BASE_URL)
                .path("/stations/" + stationId)
                .request()
                .get(StationResponse.class);
    }
}