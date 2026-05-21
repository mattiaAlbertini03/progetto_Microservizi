package me.msaproject.fleetservice.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import me.msaproject.fleetservice.dto.StartSimulationRequest;
import me.msaproject.fleetservice.dto.StopSimulationRequest;

@ApplicationScoped
public class SimulatorClient {

    private static final String SIMULATOR_SERVICE_URL =
            "http://simulator-service.acme:8080";

    private final Client client = ClientBuilder.newClient();

    public void startSimulation(StartSimulationRequest request) {
        Response response = client
                .target(SIMULATOR_SERVICE_URL)
                .path("/simulation/start")
                .request()
                .post(Entity.json(request));

        checkResponse(response, "Failed to start simulation for vehicle " + request.vehicleId());
    }

    public void stopSimulation(StopSimulationRequest request) {
        Response response = client
                .target(SIMULATOR_SERVICE_URL)
                .path("/simulation/stop")
                .request()
                .post(Entity.json(request));

        checkResponse(response, "Failed to stop simulation for vehicle " + request.vehicleId());
    }

    private void checkResponse(Response response, String errorMessage) {
        if (response.getStatus() < 200 || response.getStatus() >= 300) {
            throw new IllegalStateException(
                    errorMessage + ": HTTP " + response.getStatus()
            );
        }
    }
}