package me.msaproject.fleetservice.fleet;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;

@ApplicationScoped
@Path("/fleet")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class FleetResource {

    @Inject
    FleetService fleetService;

    @POST
    @Path("/{reservationId}/start")
    public Response startSimulation(@PathParam("reservationId") String reservationId) {

        fleetService.startSimulation(reservationId);

        return Response.accepted(Map.of(
                "reservationId", reservationId,
                "status", "simulation_started")).build();
    }

    @POST
    @Path("/{reservationId}/end")
    public Response stopSimulation(@PathParam("reservationId") String reservationId) {

        fleetService.stopSimulation(reservationId);

        return Response.ok(Map.of(
                "reservationId", reservationId,
                "status", "simulation_stopped")).build();
    }

    @GET
    @Path("/{reservationId}/at-destination")
    public Response isVehicleAtDestination(
            @PathParam("reservationId") String reservationId) {

        boolean atDestination = fleetService.isVehicleAtDestination(reservationId);

        return Response.ok(atDestination).build();
    }
}