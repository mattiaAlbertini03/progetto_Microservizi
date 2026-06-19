package me.msaproject.logisticsservice.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import me.msaproject.logisticsservice.logistics.VehicleEvent;

@ApplicationScoped
public class VehicleEventClient {

    private static final String ACME_BASE_URL =
            "http://acme-mobility-service.acme:8080";

    private final Client client = ClientBuilder.newClient();

    public void notifyVehicleUpdated(String stationId, String vehicleId) {
        client.target(ACME_BASE_URL)
                .path("/stations/" + stationId + "/vehicles/events")
                .request()
                .post(Entity.json(new VehicleEvent("VEHICLE_UPDATED", vehicleId)));
    }
}