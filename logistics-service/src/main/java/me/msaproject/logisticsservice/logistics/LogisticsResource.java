package me.msaproject.logisticsservice.logistics;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/vehicles")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class LogisticsResource {

    @Inject
    LogisticsService logisticsService;

    @GET
    public Response getVehicles() {
        return Response.ok(logisticsService.getVehicles()).build();
    }

    @GET
    @Path("/{vehicleId}")
    public Response getVehicle(
            @PathParam("vehicleId") String vehicleId) {

        return Response.ok(
                logisticsService.getVehicle(vehicleId)
        ).build();
    }

    @POST
    @Path("/{vehicleId}/hold")
    public Response holdVehicle(
            @PathParam("vehicleId") String vehicleId) {

        return Response.ok(
                logisticsService.holdVehicle(vehicleId)
        ).build();
    }

    @POST
    @Path("/{vehicleId}/pickup")
    public Response pickupVehicle(
            @PathParam("vehicleId") String vehicleId) {

        return Response.ok(
                logisticsService.pickupVehicle(vehicleId)
        ).build();
    }

    @POST
    @Path("/{vehicleId}/release")
    public Response releaseVehicle(
            @PathParam("vehicleId") String vehicleId) {

        return Response.ok(
                logisticsService.releaseVehicle(vehicleId)
        ).build();
    }

    @POST
    @Path("/{vehicleId}/start-charging")
    public Response startCharging(
            @PathParam("vehicleId") String vehicleId) {

        return Response.ok(
                logisticsService.startCharging(vehicleId)
        ).build();
    }

    @POST
    @Path("/{vehicleId}/stop-charging")
    public Response stopCharging(
            @PathParam("vehicleId") String vehicleId) {

        return Response.ok(
                logisticsService.stopCharging(vehicleId)
        ).build();
    }
}