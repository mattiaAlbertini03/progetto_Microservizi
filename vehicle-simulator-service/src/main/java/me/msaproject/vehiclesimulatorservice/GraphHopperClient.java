package me.msaproject.vehiclesimulatorservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class GraphHopperClient {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    @ConfigProperty(name = "graphhopper.base-url")
    String graphhopperBaseUrl;

    @Inject
    @ConfigProperty(name = "graphhopper.profile", defaultValue = "car")
    String profile;

    public RouteData getRoute(double fromLat, double fromLon, double toLat, double toLon) {
        try {
            String url = graphhopperBaseUrl
                    + "/route?point=" + fromLat + "," + fromLon
                    + "&point=" + toLat + "," + toLon
                    + "&profile=" + profile
                    + "&points_encoded=false";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new RuntimeException("GraphHopper error: " + response.statusCode() + " " + response.body());
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode path = root.get("paths").get(0);

            double distanceMeters = path.get("distance").asDouble();

            JsonNode coordinates = path.get("points").get("coordinates");
            List<RoutePoint> points = new ArrayList<>();

            for (JsonNode coord : coordinates) {
                double lon = coord.get(0).asDouble();
                double lat = coord.get(1).asDouble();
                points.add(new RoutePoint(lat, lon));
            }

            if (points.isEmpty()) {
                throw new RuntimeException("GraphHopper returned an empty route");
            }

            return new RouteData(points, distanceMeters);

        } catch (Exception e) {
            throw new RuntimeException("Failed to obtain route from GraphHopper", e);
        }
    }
}