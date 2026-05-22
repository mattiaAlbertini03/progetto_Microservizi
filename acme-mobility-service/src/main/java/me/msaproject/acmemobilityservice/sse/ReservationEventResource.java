package me.msaproject.acmemobilityservice.sse;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.sse.SseEventSink;
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

        reservationEventService.subscribe(requestId, eventSink);
    }

    @POST
    @Path("/reservation-requests/{requestId}/events")
    public Response publishReservationRequestEvent(
            @PathParam("requestId") String requestId,
            ReservationEvent event) {

        reservationEventService.publish(requestId, event);

        return Response.ok().build();
    }

    @GET
    @Path("/reservations/{reservationId}/events")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void listenReservationEvents(
            @PathParam("reservationId") String reservationId,
            @Context SseEventSink eventSink) {

        reservationEventService.subscribe(reservationId, eventSink);
    }

    @POST
    @Path("/reservations/{reservationId}/events")
    public Response publishReservationEvent(
            @PathParam("reservationId") String reservationId,
            ReservationEvent event) {

        reservationEventService.publish(reservationId, event);

        return Response.ok().build();
    }

    @GET
    @Path("/reservations/{reservationId}/return/events")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void listenReservationReturnEvents(
            @PathParam("reservationId") String reservationId,
            @Context SseEventSink eventSink) {

        reservationEventService.subscribe("return-" + reservationId, eventSink);
    }

    @POST
    @Path("/reservations/{reservationId}/return/events")
    public Response publishReservationReturnEvent(
            @PathParam("reservationId") String reservationId,
            ReservationEvent event) {

        reservationEventService.publish("return-" + reservationId, event);

        return Response.ok().build();
    }
}