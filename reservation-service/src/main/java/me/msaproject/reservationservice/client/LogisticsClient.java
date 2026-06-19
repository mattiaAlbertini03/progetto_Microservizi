package me.msaproject.reservationservice.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
public class LogisticsClient {

    private static final String LOGISTICS_BASE_URL = "http://logistics-service.acme:8080";

    private final Client client = ClientBuilder.newClient();

    public void holdVehicle(String vehicleId) {
        Response response = client
                .target(LOGISTICS_BASE_URL)
                .path("/vehicles/" + vehicleId + "/hold")
                .request()
                .post(Entity.json(null));

        checkResponse(response, "Failed to hold vehicle " + vehicleId);
    }

    public void pickupVehicle(String vehicleId) {
        Response response = client
                .target(LOGISTICS_BASE_URL)
                .path("/vehicles/" + vehicleId + "/pickup")
                .request()
                .post(Entity.json(null));

        checkResponse(response, "Failed to pickup vehicle " + vehicleId);
    }

    public void releaseVehicle(String vehicleId) {
        Response response = client
                .target(LOGISTICS_BASE_URL)
                .path("/vehicles/" + vehicleId + "/release")
                .request()
                .post(Entity.json(null));

        checkResponse(response, "Failed to release vehicle " + vehicleId);
    }

    public void returnVehicle(String vehicleId) {

        Response response = client
                .target(LOGISTICS_BASE_URL)
                .path("/vehicles/" + vehicleId + "/return")
                .request()
                .post(Entity.json(null));

        checkResponse(response, "Failed to return vehicle " + vehicleId);
    }

    private void checkResponse(Response response, String message) {
        if (response.getStatus() < 200 || response.getStatus() >= 300) {
            String responseBody = "";

            try {
                responseBody = response.readEntity(String.class);
            } catch (Exception ignored) {
            }

            throw new IllegalStateException(
                    message +
                            " | HTTP " + response.getStatus() +
                            (responseBody.isBlank()
                                    ? ""
                                    : " | Body: " + responseBody));
        }
    }
}