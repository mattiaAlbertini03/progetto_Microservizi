package me.msaproject.acmemobilityservice.reservation;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;

@Path("/reservations")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ReservationResource {

    @Inject
    ReservationService reservationService;

    @POST
    public Response createReservation(ReservationRequest request) {
        String reservationId = reservationService.createReservation(request);

        return Response.accepted(Map.of(
                "reservationId", reservationId,
                "status", "PENDING"
        )).build();
    }
}