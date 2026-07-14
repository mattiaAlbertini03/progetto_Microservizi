package me.msaproject.acmemobilityservice.api;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import me.msaproject.acmemobilityservice.client.LogisticsClient;

@Path("/logistics")
@Produces(MediaType.APPLICATION_JSON)
public class LogisticsApi {

    @Inject
    LogisticsClient logisticsClient;

    @GET
    @Path("/vehicles")
    public Response getVehicles(@QueryParam("stationId") String stationId) {
        if (stationId != null && !stationId.isBlank()) {
            return Response.ok(
                    logisticsClient.getVehiclesByStation(stationId)
            ).build();
        }

        return Response.ok(
                logisticsClient.getVehicles()
        ).build();
    }

    @GET
    @Path("/vehicles/{vehicleId}")
    public Response getVehicle(
            @PathParam("vehicleId") String vehicleId) {

        return Response.ok(
                logisticsClient.getVehicle(vehicleId)
        ).build();
    }
}