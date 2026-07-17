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

    public Optional<StationResponse> findStationNear(double lat, double lon) {
        return getStations().stations().stream()
                .filter(station -> distanceMeters(lat, lon, station.lat(), station.lon()) <= 30.0)
                .findFirst();
    }

    private double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double earthRadius = 6_371_000;
        double latDelta = Math.toRadians(lat2 - lat1);
        double lonDelta = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDelta / 2) * Math.sin(latDelta / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDelta / 2) * Math.sin(lonDelta / 2);
        return earthRadius * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
