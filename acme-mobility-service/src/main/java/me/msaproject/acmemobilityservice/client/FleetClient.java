package me.msaproject.acmemobilityservice.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
public class FleetClient {

    private static final String FLEET_BASE_URL = "http://fleet-service.acme:8080";

    private final Client client = ClientBuilder.newClient();

    public boolean isVehicleAtDestination(String reservationId) {

        Response response = client
                .target(FLEET_BASE_URL)
                .path("/fleet/" + reservationId + "/at-destination")
                .request()
                .get();

        checkResponse(
                response,
                "Failed to check vehicle destination for reservation " + reservationId
        );

        return response.readEntity(Boolean.class);
    }

    private void checkResponse(Response response, String errorMessage) {
        if (response.getStatus() < 200 || response.getStatus() >= 300) {
            throw new IllegalStateException(
                    errorMessage + ": HTTP " + response.getStatus()
            );
        }
    }
}