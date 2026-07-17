package me.msaproject.fleetservice.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import me.msaproject.fleetservice.dto.Vehicle;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Map;

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

    public Vehicle getVehicle(String vehicleId) {
        return client
                .target(logisticsServiceUrl)
                .path("vehicles")
                .path(vehicleId)
                .request()
                .get(Vehicle.class);
    }

    public Vehicle updateVehicleStatus(String vehicleId, Map<String, Object> request) {
        return client
                .target(logisticsServiceUrl)
                .path("vehicles")
                .path(vehicleId)
                .path("update-status")
                .request()
                .post(Entity.json(request), Vehicle.class);
    }
}
