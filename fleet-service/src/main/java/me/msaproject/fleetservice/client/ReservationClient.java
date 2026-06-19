package me.msaproject.fleetservice.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import me.msaproject.fleetservice.dto.Reservation;

@ApplicationScoped
public class ReservationClient {

    private static final String RESERVATION_SERVICE_URL =
            "http://reservation-service.acme:8080";

    private final Client client = ClientBuilder.newClient();

    public Reservation getReservation(String reservationId) {
        return client
                .target(RESERVATION_SERVICE_URL)
                .path("/reservations/" + reservationId)
                .request()
                .get(Reservation.class);
    }
}