package me.msaproject.acmemobilityservice.client;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import me.msaproject.acmemobilityservice.dto.reservation.ReservationRequest;
import me.msaproject.acmemobilityservice.dto.reservation.ReservationResponse;
import me.msaproject.acmemobilityservice.dto.reservation.ReservationStatusResponse;

@ApplicationScoped
public class ReservationsClient {

    private static final String RESERVATION_SERVICE_BASE_URL = "http://reservation-service.acme:8080";

    private final Client client = ClientBuilder.newClient();

    public ReservationResponse createReservation(ReservationRequest request) {
        Response response = client
                .target(RESERVATION_SERVICE_BASE_URL)
                .path("/reservations")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(request));

        checkResponse(response, "Failed to create reservation");

        return response.readEntity(ReservationResponse.class);
    }

    public ReservationResponse getReservation(String reservationId) {

        Response response = client
                .target(RESERVATION_SERVICE_BASE_URL)
                .path("/reservations/" + reservationId)
                .request(MediaType.APPLICATION_JSON)
                .get();

        checkResponse(
                response,
                "Failed to get reservation " + reservationId);

        return response.readEntity(ReservationResponse.class);
    }

    public ReservationStatusResponse holdReservation(String reservationId) {
        Response response = client
                .target(RESERVATION_SERVICE_BASE_URL)
                .path("/reservations/" + reservationId + "/hold")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(null));

        checkResponse(response, "Failed to hold reservation " + reservationId);

        return response.readEntity(ReservationStatusResponse.class);
    }

    public ReservationStatusResponse pickupReservation(String reservationId) {
        Response response = client
                .target(RESERVATION_SERVICE_BASE_URL)
                .path("/reservations/" + reservationId + "/pickup")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(null));

        checkResponse(response, "Failed to pickup reservation " + reservationId);

        return response.readEntity(ReservationStatusResponse.class);
    }

    public ReservationStatusResponse cancelReservation(String reservationId) {
        Response response = client
                .target(RESERVATION_SERVICE_BASE_URL)
                .path("/reservations/" + reservationId + "/cancel")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(null));

        checkResponse(response, "Failed to cancel reservation " + reservationId);

        return response.readEntity(ReservationStatusResponse.class);
    }

    public List<ReservationResponse> getReservationsByUserId(String userId) {
        return client
                .target(RESERVATION_SERVICE_BASE_URL)
                .path("/reservations")
                .queryParam("userId", userId)
                .request()
                .get(new GenericType<List<ReservationResponse>>() {
                });
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