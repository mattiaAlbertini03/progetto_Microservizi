package me.msaproject.acmemobilityservice.vehicle;

import java.util.Map;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/vehicles")
@Produces(MediaType.APPLICATION_JSON)
public class VehicleResource {

    private final VehicleService vehicleService;

    @Inject
    public VehicleResource(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @GET
    public Response getVehicles(@QueryParam("stationId") String stationId) {
        if (stationId != null && !stationId.isBlank()) {
            return Response.ok(vehicleService.getVehiclesByStation(stationId)).build();
        }

        return Response.ok(vehicleService.getVehicles()).build();
    }

    @GET
    @Path("/{id}")
    public Response getVehicleById(@PathParam("id") String id) {
        return vehicleService.getVehicleById(id)
                .map(vehicle -> Response.ok(vehicle).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    @GET
    @Path("/qr/{qrCode}")
    public Response getVehicleByQrCode(@PathParam("qrCode") String qrCode) {
        return vehicleService.getVehicleByQrCode(qrCode)
                .map(vehicle -> Response.ok(vehicle).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    @POST
    @Path("/{vehicleId}/reserve")
    public Response reserveVehicle(@PathParam("vehicleId") String vehicleId) {
        System.out.println("Reserve vehicle requested: " + vehicleId);

        return Response.accepted(Map.of(
                "vehicleId", vehicleId,
                "status", "RESERVED")).build();
    }
}