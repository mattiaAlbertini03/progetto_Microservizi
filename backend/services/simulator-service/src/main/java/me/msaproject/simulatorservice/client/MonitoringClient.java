package me.msaproject.simulatorservice.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import me.msaproject.simulatorservice.dto.VehicleBatteryEvent;
import me.msaproject.simulatorservice.dto.VehicleTrackingEvent;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@ApplicationScoped
public class MonitoringClient {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String trackingServiceUrl;
    private final String batteryServiceUrl;

    @Inject
    public MonitoringClient(
            @ConfigProperty(
                    name = "tracking-service.url",
                    defaultValue = "http://tracking-service.acme:8080"
            )
            String trackingServiceUrl,

            @ConfigProperty(
                    name = "battery-service.url",
                    defaultValue = "http://battery-service.acme:8080"
            )
            String batteryServiceUrl
    ) {
        this.trackingServiceUrl = trackingServiceUrl;
        this.batteryServiceUrl = batteryServiceUrl;
    }

    public void sendTracking(VehicleTrackingEvent event) {
        post(
                trackingServiceUrl + "/tracking",
                event
        );
    }

    public void sendBattery(VehicleBatteryEvent event) {
        post(
                batteryServiceUrl + "/battery",
                event
        );
    }

    private void post(String url, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() < 200
                    || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "POST failed to "
                                + url
                                + " | HTTP "
                                + response.statusCode()
                                + " | Body: "
                                + response.body()
                );
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Monitoring call was interrupted: " + url,
                    e
            );
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Monitoring call failed to " + url,
                    e
            );
        }
    }
}