package me.msaproject.simulatorservice.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import me.msaproject.simulatorservice.dto.RouteData;
import me.msaproject.simulatorservice.dto.RoutePoint;
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

    private final String graphhopperBaseUrl;
    private final String profile;

    @Inject
    public GraphHopperClient(
            @ConfigProperty(
                    name = "graphhopper.base-url",
                    defaultValue = "http://graphhopper-service.simulation:8989"
            )
            String graphhopperBaseUrl,

            @ConfigProperty(
                    name = "graphhopper.profile",
                    defaultValue = "car"
            )
            String profile
    ) {
        this.graphhopperBaseUrl = graphhopperBaseUrl;
        this.profile = profile;
    }

    public RouteData getRoute(
            double fromLat,
            double fromLon,
            double toLat,
            double toLon
    ) {
        String url = graphhopperBaseUrl
                + "/route?point=" + fromLat + "," + fromLon
                + "&point=" + toLat + "," + toLon
                + "&profile=" + profile
                + "&points_encoded=false";

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "GraphHopper returned HTTP "
                                + response.statusCode()
                                + ": "
                                + response.body()
                );
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode paths = root.get("paths");

            if (paths == null || !paths.isArray() || paths.isEmpty()) {
                throw new IllegalStateException(
                        "GraphHopper returned no route"
                );
            }

            JsonNode path = paths.get(0);
            double distanceMeters = path.get("distance").asDouble();

            JsonNode coordinates = path
                    .get("points")
                    .get("coordinates");

            List<RoutePoint> points = new ArrayList<>();

            for (JsonNode coordinate : coordinates) {
                double lon = coordinate.get(0).asDouble();
                double lat = coordinate.get(1).asDouble();

                points.add(new RoutePoint(lat, lon));
            }

            if (points.isEmpty()) {
                throw new IllegalStateException(
                        "GraphHopper returned an empty route"
                );
            }

            return new RouteData(points, distanceMeters);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "GraphHopper request was interrupted",
                    e
            );
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to obtain route from GraphHopper",
                    e
            );
        }
    }
}