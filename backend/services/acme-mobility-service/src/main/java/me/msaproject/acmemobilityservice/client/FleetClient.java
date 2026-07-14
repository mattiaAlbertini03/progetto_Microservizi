package me.msaproject.acmemobilityservice.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class FleetClient {

    private final Client client = ClientBuilder.newClient();
    private final String fleetServiceUrl;

    @Inject
    public FleetClient(
            @ConfigProperty(
                    name = "fleet-service.url",
                    defaultValue = "http://fleet-service.acme:8080"
            )
            String fleetServiceUrl
    ) {
        this.fleetServiceUrl = fleetServiceUrl;
    }

    public boolean isVehicleAtDestination(String reservationId) {
        try (Response response = client
                .target(fleetServiceUrl)
                .path("fleet")
                .path(reservationId)
                .path("at-destination")
                .request()
                .get()) {

            checkResponse(
                    response,
                    "Failed to check vehicle destination for reservation "
                            + reservationId
            );

            return response.readEntity(Boolean.class);
        }
    }

    private void checkResponse(Response response, String errorMessage) {
        if (response.getStatusInfo().getFamily()
                != Response.Status.Family.SUCCESSFUL) {
            throw new IllegalStateException(
                    errorMessage + ": HTTP " + response.getStatus()
            );
        }
    }
}