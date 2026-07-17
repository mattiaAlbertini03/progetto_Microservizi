package me.msaproject.simulatorservice.simulation;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import me.msaproject.simulatorservice.dto.StartSimulationRequest;
import me.msaproject.simulatorservice.dto.StopSimulationRequest;
import me.msaproject.simulatorservice.dto.SimulationSummary;

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

        SimulationSummary summary = simulationService.stop(request.vehicleId());

        return Response.ok(summary)
                .build();
    }

    @GET
    @Path("/{vehicleId}")
    public Response active(@PathParam("vehicleId") String vehicleId) {
        return Response.ok(simulationService.getStatus(vehicleId)).build();
    }

    @POST
    @Path("/{vehicleId}/recharge-and-resume")
    public Response rechargeAndResume(@PathParam("vehicleId") String vehicleId) {
        simulationService.rechargeAndResume(vehicleId);
        return Response.ok("{\"status\":\"resumed\",\"batteryPct\":25}").build();
    }
}
