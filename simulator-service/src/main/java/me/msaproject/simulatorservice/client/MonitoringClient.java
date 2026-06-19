package me.msaproject.vehiclesimulatorservice;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.inject.Inject;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@ApplicationScoped
public class MonitoringClient {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    @ConfigProperty(name = "tracking.base-url")
    String trackingBaseUrl;

    @Inject
    @ConfigProperty(name = "battery.base-url")
    String batteryBaseUrl;

    public void sendTracking(VehicleTrackingEvent event) {
        post(trackingBaseUrl + "/tracking", event);
    }

    public void sendBattery(VehicleBatteryEvent event) {
        post(batteryBaseUrl + "/battery", event);
    }

    private void post(String url, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new RuntimeException(
                        "POST failed to " + url +
                        " status=" + response.statusCode() +
                        " body=" + response.body()
                );
            }

        } catch (Exception e) {
            throw new RuntimeException("Monitoring call failed to " + url, e);
        }
    }
}