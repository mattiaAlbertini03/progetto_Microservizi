package me.msaproject.acmemobilityservice.api;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import me.msaproject.acmemobilityservice.client.StationsClient;

@Path("/stations")
@Produces(MediaType.APPLICATION_JSON)
public class StationsApi {

    @Inject
    StationsClient stationClient;

    @GET
    public Response getStations() {
        return Response.ok(stationClient.getStations()).build();
    }
}