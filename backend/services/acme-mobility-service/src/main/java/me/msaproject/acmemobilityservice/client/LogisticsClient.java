package me.msaproject.acmemobilityservice.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import me.msaproject.acmemobilityservice.dto.logistics.VehicleResponse;
import me.msaproject.acmemobilityservice.dto.station.StationResponse;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.List;

@ApplicationScoped
public class LogisticsClient {

    private final Client client = ClientBuilder.newClient();
    private final StationsClient stationsClient;
    private final String logisticsServiceUrl;

    @Inject
    public LogisticsClient(
            StationsClient stationsClient,
            @ConfigProperty(
                    name = "logistics-service.url",
                    defaultValue = "http://logistics-service:8080"
            )
            String logisticsServiceUrl
    ) {
        this.stationsClient = stationsClient;
        this.logisticsServiceUrl = logisticsServiceUrl;
    }

    public List<VehicleResponse> getVehicles() {
        try (Response response = client
                .target(logisticsServiceUrl)
                .path("vehicles")
                .request(MediaType.APPLICATION_JSON)
                .get()) {

            checkResponse(response, "Failed to get vehicles");

            return response.readEntity(
                    new GenericType<List<VehicleResponse>>() {
                    }
            );
        }
    }

    public List<VehicleResponse> getVehiclesByStation(String stationId) {
        StationResponse station = stationsClient.getStation(stationId);

        try (Response response = client
                .target(logisticsServiceUrl)
                .path("vehicles")
                .queryParam("lat", station.lat())
                .queryParam("lon", station.lon())
                .request(MediaType.APPLICATION_JSON)
                .get()) {

            checkResponse(
                    response,
                    "Failed to get vehicles for station " + stationId
            );

            return response.readEntity(
                    new GenericType<List<VehicleResponse>>() {
                    }
            );
        }
    }

    public VehicleResponse getVehicle(String vehicleId) {
        try (Response response = client
                .target(logisticsServiceUrl)
                .path("vehicles")
                .path(vehicleId)
                .request(MediaType.APPLICATION_JSON)
                .get()) {

            checkResponse(
                    response,
                    "Failed to get vehicle " + vehicleId
            );

            return response.readEntity(VehicleResponse.class);
        }
    }

    private void checkResponse(Response response, String message) {
        if (response.getStatusInfo().getFamily()
                == Response.Status.Family.SUCCESSFUL) {
            return;
        }

        String errorBody = null;

        try {
            errorBody = response.readEntity(String.class);
        } catch (Exception ignored) {
        }

        throw new IllegalStateException(
                message
                        + ". Status: "
                        + response.getStatus()
                        + (errorBody != null && !errorBody.isBlank()
                        ? ". Body: " + errorBody
                        : "")
        );
    }
}