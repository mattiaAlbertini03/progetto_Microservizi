package me.msaproject.acmemobilityservice.api;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import me.msaproject.acmemobilityservice.client.AuthClient;
import me.msaproject.acmemobilityservice.dto.auth.LoginRequest;
import me.msaproject.acmemobilityservice.dto.auth.SignupRequest;

@Path("/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthApi {

    @Inject
    AuthClient authClient;

    @POST
    @Path("/login")
    public Response login(LoginRequest request) {
        return Response.ok(authClient.login(request)).build();
    }

    @POST
    @Path("/signup")
    public Response signup(SignupRequest request) {
        return Response.status(Response.Status.CREATED)
                .entity(authClient.signup(request))
                .build();
    }
}