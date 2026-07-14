package me.msaproject.reservationservice.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class LogisticsClient {

    private final Client client = ClientBuilder.newClient();
    private final String logisticsServiceUrl;

    @Inject
    public LogisticsClient(
            @ConfigProperty(
                    name = "logistics-service.url",
                    defaultValue = "http://logistics-service.acme:8080"
            )
            String logisticsServiceUrl
    ) {
        this.logisticsServiceUrl = logisticsServiceUrl;
    }

    public void holdVehicle(String vehicleId) {
        executeVehicleAction(
                vehicleId,
                "hold",
                "Failed to hold vehicle " + vehicleId
        );
    }

    public void pickupVehicle(String vehicleId) {
        executeVehicleAction(
                vehicleId,
                "pickup",
                "Failed to pickup vehicle " + vehicleId
        );
    }

    public void releaseVehicle(String vehicleId) {
        executeVehicleAction(
                vehicleId,
                "release",
                "Failed to release vehicle " + vehicleId
        );
    }

    public void returnVehicle(String vehicleId) {
        executeVehicleAction(
                vehicleId,
                "return",
                "Failed to return vehicle " + vehicleId
        );
    }

    private void executeVehicleAction(
            String vehicleId,
            String action,
            String errorMessage
    ) {
        try (Response response = client
                .target(logisticsServiceUrl)
                .path("vehicles")
                .path(vehicleId)
                .path(action)
                .request()
                .post(Entity.json(null))) {

            checkResponse(response, errorMessage);
        }
    }

    private void checkResponse(Response response, String message) {
        if (response.getStatusInfo().getFamily()
                == Response.Status.Family.SUCCESSFUL) {
            return;
        }

        String responseBody = "";

        try {
            responseBody = response.readEntity(String.class);
        } catch (Exception ignored) {
        }

        throw new IllegalStateException(
                message
                        + " | HTTP "
                        + response.getStatus()
                        + (responseBody.isBlank()
                        ? ""
                        : " | Body: " + responseBody)
        );
    }
}