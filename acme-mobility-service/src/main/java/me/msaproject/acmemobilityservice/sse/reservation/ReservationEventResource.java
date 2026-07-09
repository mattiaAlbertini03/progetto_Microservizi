package me.msaproject.acmemobilityservice.sse.reservation;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.sse.SseEventSink;
import me.msaproject.acmemobilityservice.dto.reservation.ReservationCreationEvent;
import me.msaproject.acmemobilityservice.dto.reservation.ReservationEvent;

@Path("/")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ReservationEventResource {

    @Inject
    ReservationEventService reservationEventService;

    @GET
    @Path("/reservation-requests/{requestId}/events")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void listenReservationRequestEvents(
            @PathParam("requestId") String requestId,
            @Context SseEventSink eventSink) {

        reservationEventService.subscribe("request-" + requestId, eventSink);
    }

    @POST
    @Path("/reservation-requests/{requestId}/events")
    public Response publishReservationRequestEvent(
            @PathParam("requestId") String requestId,
            ReservationCreationEvent event) {

        reservationEventService.publish("request-" + requestId, event);

        return Response.ok().build();
    }

    @GET
    @Path("/reservations/{reservationId}/events")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void listenReservationEvents(
            @PathParam("reservationId") String reservationId,
            @Context SseEventSink eventSink) {

        reservationEventService.subscribe("reservation-" + reservationId, eventSink);
    }

    @POST
    @Path("/reservations/{reservationId}/events")
    public Response publishReservationEvent(
            @PathParam("reservationId") String reservationId,
            ReservationEvent event) {

        reservationEventService.publish("reservation-" + reservationId, event);

        return Response.ok().build();
    }
}