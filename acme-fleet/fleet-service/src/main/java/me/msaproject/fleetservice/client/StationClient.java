package me.msaproject.fleetservice.client;

import java.util.Arrays;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import me.msaproject.fleetservice.dto.Station;

@ApplicationScoped
public class StationClient {

    private static final String STATION_SERVICE_URL =
            "http://station-service.acme:8080";

    private final Client client = ClientBuilder.newClient();

    public List<Station> getStations() {
        Station[] stations = client
                .target(STATION_SERVICE_URL)
                .path("/stations")
                .request()
                .get(Station[].class);

        return Arrays.asList(stations);
    }
}