package me.msaproject.acmemobilityservice.sse.vehicle;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.sse.SseEventSink;
import me.msaproject.acmemobilityservice.dto.logistics.VehicleEvent;

@Path("/")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class VehicleEventResource {

    @Inject
    VehicleEventService vehicleEventService;

    @GET
    @Path("/stations/{stationId}/vehicles/events")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void listenVehicleEvents(
            @PathParam("stationId") String stationId,
            @Context SseEventSink eventSink) {

        vehicleEventService.subscribe(stationId, eventSink);
    }

    @POST
    @Path("/stations/{stationId}/vehicles/events")
    public Response publishVehicleEvent(
            @PathParam("stationId") String stationId,
            VehicleEvent event) {

        vehicleEventService.publish(stationId, event);

        return Response.ok().build();
    }
}