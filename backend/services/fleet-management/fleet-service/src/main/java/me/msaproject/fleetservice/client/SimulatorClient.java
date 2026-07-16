package me.msaproject.fleetservice.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import me.msaproject.fleetservice.dto.StartSimulationRequest;
import me.msaproject.fleetservice.dto.StopSimulationRequest;
import me.msaproject.fleetservice.dto.SimulationSummary;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class SimulatorClient {

    private final Client client = ClientBuilder.newClient();
    private final String simulatorServiceUrl;

    @Inject
    public SimulatorClient(
            @ConfigProperty(
                    name = "simulator-service.url",
                    defaultValue = "http://simulator-service.acme:8080"
            )
            String simulatorServiceUrl
    ) {
        this.simulatorServiceUrl = simulatorServiceUrl;
    }

    public void startSimulation(StartSimulationRequest request) {
        try (Response response = client
                .target(simulatorServiceUrl)
                .path("simulations")
                .path("start")
                .request()
                .post(Entity.json(request))) {

            checkResponse(
                    response,
                    "Failed to start simulation for vehicle "
                            + request.vehicleId()
            );
        }
    }

    public SimulationSummary stopSimulation(StopSimulationRequest request) {
        try (Response response = client
                .target(simulatorServiceUrl)
                .path("simulations")
                .path("stop")
                .request()
                .post(Entity.json(request))) {

            checkResponse(
                    response,
                    "Failed to stop simulation for vehicle "
                            + request.vehicleId()
            );

            return response.readEntity(SimulationSummary.class);
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
