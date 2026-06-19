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

import java.util.List;

@ApplicationScoped
public class LogisticsClient {
    @Inject
    StationsClient stationsClient;

    private static final String LOGISTICS_SERVICE_BASE_URL = "http://logistics-service:8080";

    private final Client client = ClientBuilder.newClient();

    public List<VehicleResponse> getVehicles() {
        Response response = client
                .target(LOGISTICS_SERVICE_BASE_URL)
                .path("/vehicles")
                .request(MediaType.APPLICATION_JSON)
                .get();

        checkResponse(response, "Failed to get vehicles");

        return response.readEntity(new GenericType<List<VehicleResponse>>() {
        });
    }

    public List<VehicleResponse> getVehiclesByStation(String stationId) {
        StationResponse station = stationsClient.getStation(stationId);

        return client
                .target(LOGISTICS_SERVICE_BASE_URL)
                .path("/vehicles")
                .queryParam("lat", station.lat())
                .queryParam("lon", station.lon())
                .request()
                .get(new GenericType<List<VehicleResponse>>() {
                });
    }

    public VehicleResponse getVehicle(String vehicleId) {
        Response response = client
                .target(LOGISTICS_SERVICE_BASE_URL)
                .path("/vehicles/" + vehicleId)
                .request(MediaType.APPLICATION_JSON)
                .get();

        checkResponse(response, "Failed to get vehicle " + vehicleId);

        return response.readEntity(VehicleResponse.class);
    }

    private void checkResponse(Response response, String message) {
        if (response.getStatus() < 200 || response.getStatus() >= 300) {
            String errorBody = null;

            try {
                errorBody = response.readEntity(String.class);
            } catch (Exception ignored) {
            }

            throw new IllegalStateException(
                    message +
                            ". Status: " + response.getStatus() +
                            (errorBody != null && !errorBody.isBlank()
                                    ? ". Body: " + errorBody
                                    : ""));
        }
    }
}