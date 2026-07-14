package me.msaproject.acmemobilityservice.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import me.msaproject.acmemobilityservice.dto.reservation.ReservationCreationRequest;
import me.msaproject.acmemobilityservice.dto.reservation.ReservationResponse;
import me.msaproject.acmemobilityservice.dto.reservation.ReservationStatusResponse;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.List;

@ApplicationScoped
public class ReservationsClient {

    private final Client client = ClientBuilder.newClient();
    private final String reservationServiceUrl;

    @Inject
    public ReservationsClient(
            @ConfigProperty(
                    name = "reservation-service.url",
                    defaultValue = "http://reservation-service.acme:8080"
            )
            String reservationServiceUrl
    ) {
        this.reservationServiceUrl = reservationServiceUrl;
    }

    public ReservationResponse createReservation(
            ReservationCreationRequest request
    ) {
        try (Response response = client
                .target(reservationServiceUrl)
                .path("reservations")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(request))) {

            checkResponse(response, "Failed to create reservation");

            return response.readEntity(ReservationResponse.class);
        }
    }

    public ReservationResponse getReservation(String reservationId) {
        try (Response response = client
                .target(reservationServiceUrl)
                .path("reservations")
                .path(reservationId)
                .request(MediaType.APPLICATION_JSON)
                .get()) {

            checkResponse(
                    response,
                    "Failed to get reservation " + reservationId
            );

            return response.readEntity(ReservationResponse.class);
        }
    }

    public ReservationStatusResponse holdReservation(
            String reservationId
    ) {
        return executeReservationAction(
                reservationId,
                "hold",
                "Failed to hold reservation " + reservationId
        );
    }

    public ReservationStatusResponse pickupReservation(
            String reservationId
    ) {
        return executeReservationAction(
                reservationId,
                "pickup",
                "Failed to pickup reservation " + reservationId
        );
    }

    public ReservationStatusResponse cancelReservation(
            String reservationId
    ) {
        return executeReservationAction(
                reservationId,
                "cancel",
                "Failed to cancel reservation " + reservationId
        );
    }

    public List<ReservationResponse> getReservationsByUserId(
            String userId
    ) {
        try (Response response = client
                .target(reservationServiceUrl)
                .path("reservations")
                .queryParam("userId", userId)
                .request(MediaType.APPLICATION_JSON)
                .get()) {

            checkResponse(
                    response,
                    "Failed to get reservations for user " + userId
            );

            return response.readEntity(
                    new GenericType<List<ReservationResponse>>() {
                    }
            );
        }
    }

    private ReservationStatusResponse executeReservationAction(
            String reservationId,
            String action,
            String errorMessage
    ) {
        try (Response response = client
                .target(reservationServiceUrl)
                .path("reservations")
                .path(reservationId)
                .path(action)
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(null))) {

            checkResponse(response, errorMessage);

            return response.readEntity(ReservationStatusResponse.class);
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