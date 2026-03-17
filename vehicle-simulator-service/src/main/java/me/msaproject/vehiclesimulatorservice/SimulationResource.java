package me.msaproject.vehiclesimulatorservice;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/simulations")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class SimulationResource {

    @Inject
    SimulationService simulationService;

    @POST
    @Path("/start")
    public Response start(StartSimulationRequest request) {

        simulationService.start(request);

        return Response.accepted()
                .entity("{\"status\":\"started\"}")
                .build();
    }

    @POST
    @Path("/stop")
    public Response stop(StopSimulationRequest request) {

        simulationService.stop(request.vehicleId());

        return Response.ok("{\"status\":\"stopped\"}")
                .build();
    }

    @GET
    @Path("/{vehicleId}")
    public Response active(@PathParam("vehicleId") String vehicleId) {

        boolean active = simulationService.isActive(vehicleId);

        return Response.ok("""
                {"vehicleId":"%s","active":%s}
                """.formatted(vehicleId, active)).build();
    }
}