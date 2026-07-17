package me.msaproject.acmemobilityservice.sse.simulation;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.sse.SseEventSink;
import me.msaproject.acmemobilityservice.dto.reservation.SimulationVisualizerEvent;

@Path("/simulation-visualiser/{reservationId}/events")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class SimulationVisualizerStreamResource {

    @Inject
    SimulationVisualizerEventService eventService;

    /** Keeps the visualizer subscribed to calculated trip telemetry. */
    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void listen(
            @PathParam("reservationId") String reservationId,
            @Context SseEventSink sink) {
        eventService.subscribe(reservationId, sink);
    }

    /** Receives a BPMN telemetry update and forwards it to the active browser. */
    @POST
    public Response publish(
            @PathParam("reservationId") String reservationId,
            SimulationVisualizerEvent event) {
        if (event == null || !reservationId.equals(event.reservationId())) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("reservationId does not match path").build();
        }
        eventService.publish(reservationId, event);
        return Response.ok().build();
    }
}
