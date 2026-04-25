package me.msaproject.acmeworkerservice.station;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/stations")
@Produces(MediaType.APPLICATION_JSON)
public class StationResource {

    private final StationService stationService;

    @Inject
    public StationResource(StationService stationService) {
        this.stationService = stationService;
    }

    @GET
    public Response getStations() {
        return Response.ok(stationService.getStations()).build();
    }

    @GET
    @Path("/{id}")
    public Response getStationById(@PathParam("id") String id) {
        return stationService.getStationById(id)
                .map(station -> Response.ok(station).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }
}