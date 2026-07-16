package me.msaproject.acmemobilityservice.sse.reservation;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.sse.SseEventSink;
import me.msaproject.acmemobilityservice.dto.reservation.ReservationEvent;

@Path("/reservations/{reservationId}/events")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ReservationStreamResource {

    @Inject
    ReservationEventService reservationEventService;

    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void listenReservationEvents(
            @PathParam("reservationId") String reservationId,
            @Context SseEventSink eventSink) {

        reservationEventService.subscribe("reservation-" + reservationId, eventSink);
    }

    @POST
    public Response publishReservationEvent(
            @PathParam("reservationId") String reservationId,
            ReservationEvent event) {

        reservationEventService.publish("reservation-" + reservationId, event);

        return Response.ok().build();
    }
}
