package me.msaproject.logisticsservice.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import me.msaproject.logisticsservice.logistics.VehicleEvent;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class VehicleEventClient {

    private final Client client = ClientBuilder.newClient();
    private final String acmeMobilityServiceUrl;

    @Inject
    public VehicleEventClient(
            @ConfigProperty(
                    name = "acme-mobility-service.url",
                    defaultValue = "http://acme-mobility-service.acme:8080"
            )
            String acmeMobilityServiceUrl
    ) {
        this.acmeMobilityServiceUrl = acmeMobilityServiceUrl;
    }

    public void notifyVehicleUpdated(String stationId, String vehicleId) {
        try (Response response = client
                .target(acmeMobilityServiceUrl)
                .path("stations")
                .path(stationId)
                .path("vehicles")
                .path("events")
                .request()
                .post(Entity.json(
                        new VehicleEvent("VEHICLE_UPDATED", vehicleId)
                ))) {

            if (response.getStatusInfo().getFamily()
                    != Response.Status.Family.SUCCESSFUL) {
                throw new IllegalStateException(
                        "Failed to notify vehicle update for vehicle "
                                + vehicleId
                                + ": HTTP "
                                + response.getStatus()
                );
            }
        }
    }
}