package me.msaproject.acmemobilityservice.reservation;

import io.camunda.client.CamundaClient;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;

import java.util.Map;

@Path("/reservations")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ReservationResource {

    @Inject
    ReservationService reservationService;

    @Inject
    ReservationEventService reservationEventService;

    @Inject
    CamundaClient camundaClient;

    @Context
    Sse sse;

    @POST
    public Response createReservation(ReservationRequest request) {
        String reservationId = reservationService.createReservation(request);

        camundaClient
                .newPublishMessageCommand()
                .messageName("ONLINE_RESERVATION_REQUEST")
                .correlationKey(request.userId())
                .variables(Map.of(
                        "reservationId", reservationId,
                        "vehicleId", request.vehicleId(),
                        "userId", request.userId()
                ))
                .send()
                .join();

        return Response.accepted(Map.of(
                "reservationId", reservationId,
                "status", "PENDING"
        )).build();
    }

    @GET
    @Path("/{reservationId}/events")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void listenReservationEvents(
            @PathParam("reservationId") String reservationId,
            @Context SseEventSink eventSink) {
        reservationEventService.subscribe(reservationId, eventSink);
    }

    @POST
    @Path("/{reservationId}/events")
    public Response publishReservationEvent(
            @PathParam("reservationId") String reservationId,
            ReservationEvent event) {
        reservationEventService.publish(reservationId, event, sse);

        return Response.accepted(Map.of(
                "status", "EVENT_PUBLISHED"
        )).build();
    }
}