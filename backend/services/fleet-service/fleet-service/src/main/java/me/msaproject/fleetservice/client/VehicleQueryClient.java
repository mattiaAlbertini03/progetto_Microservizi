package me.msaproject.fleetservice.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import me.msaproject.fleetservice.dto.VehicleSnapshot;
import me.msaproject.fleetservice.dto.VehicleTrackingResponse;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class VehicleQueryClient {

    private final Client client = ClientBuilder.newClient();
    private final String queryServiceUrl;

    @Inject
    public VehicleQueryClient(
            @ConfigProperty(
                    name = "query-service.url",
                    defaultValue = "http://query-service.acme:8080"
            )
            String queryServiceUrl
    ) {
        this.queryServiceUrl = queryServiceUrl;
    }

    public VehicleSnapshot getVehicle(String vehicleId) {
        return client
                .target(queryServiceUrl)
                .path("vehicles")
                .path(vehicleId)
                .request()
                .get(VehicleSnapshot.class);
    }

    public VehicleTrackingResponse getVehicleTracking(String vehicleId) {
        return client
                .target(queryServiceUrl)
                .path("vehicles")
                .path(vehicleId)
                .path("tracking")
                .request()
                .get(VehicleTrackingResponse.class);
    }
}