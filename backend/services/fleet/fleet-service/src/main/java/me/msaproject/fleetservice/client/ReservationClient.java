package me.msaproject.fleetservice.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import me.msaproject.fleetservice.dto.Reservation;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class ReservationClient {

    private final Client client = ClientBuilder.newClient();
    private final String reservationServiceUrl;

    @Inject
    public ReservationClient(
            @ConfigProperty(
                    name = "reservation-service.url",
                    defaultValue = "http://reservation-service.acme:8080"
            )
            String reservationServiceUrl
    ) {
        this.reservationServiceUrl = reservationServiceUrl;
    }

    public Reservation getReservation(String reservationId) {
        return client
                .target(reservationServiceUrl)
                .path("reservations")
                .path(reservationId)
                .request()
                .get(Reservation.class);
    }
}