package me.msaproject.acmemobilityservice.sse.reservation;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.sse.SseEventSink;
import me.msaproject.acmemobilityservice.dto.reservation.ReservationCreationEvent;

@Path("/reservation-requests/{requestId}/events")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ReservationEventResource {

    @Inject
    ReservationEventService reservationEventService;

    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void listenReservationRequestEvents(
            @PathParam("requestId") String requestId,
            @Context SseEventSink eventSink) {

        reservationEventService.subscribe("request-" + requestId, eventSink);
    }

    @POST
    public Response publishReservationRequestEvent(
            @PathParam("requestId") String requestId,
            ReservationCreationEvent event) {

        reservationEventService.publish("request-" + requestId, event);

        return Response.ok().build();
    }
}
