package me.msaproject.fleetservice.fleet;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import me.msaproject.fleetservice.client.LogisticsClient;

import java.util.Map;

/** Compatibility route for process instances started before the BPMN URL fix. */
@Path("/vehicles")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class LegacyVehicleResource {

    @Inject
    LogisticsClient logisticsClient;

    @POST
    @Path("/{vehicleId}/update-status")
    public Response updateVehicleStatus(
            @PathParam("vehicleId") String vehicleId,
            Map<String, Object> request
    ) {
        return Response.ok(logisticsClient.updateVehicleStatus(vehicleId, request)).build();
    }
}
