package me.msaproject.acmemobilityservice.config;

import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@Path("/")
public class OptionsResource {

    @OPTIONS
    @Path("{path: .*}")
    public Response options() {
        return Response.ok().build();
    }
}