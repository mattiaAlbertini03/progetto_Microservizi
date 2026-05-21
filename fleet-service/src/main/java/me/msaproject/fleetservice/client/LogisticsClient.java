package me.msaproject.fleetservice.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import me.msaproject.fleetservice.dto.Vehicle;

@ApplicationScoped
public class LogisticsClient {

    private static final String LOGISTICS_SERVICE_URL =
            "http://logistics-service.acme:8080";

    private final Client client = ClientBuilder.newClient();

    public Vehicle getVehicle(String vehicleId) {
        return client
                .target(LOGISTICS_SERVICE_URL)
                .path("/vehicles/" + vehicleId)
                .request()
                .get(Vehicle.class);
    }
}