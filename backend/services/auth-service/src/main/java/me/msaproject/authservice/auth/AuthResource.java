package me.msaproject.authservice.auth;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import me.msaproject.authservice.dto.LoginRequest;
import me.msaproject.authservice.dto.SignupRequest;

@Path("/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject
    AuthService authService;

    @POST
    @Path("/login")
    public Response login(LoginRequest request) {
        return Response.ok(authService.login(request)).build();
    }

    @POST
    @Path("/signup")
    public Response signup(SignupRequest request) {
        return Response.status(Response.Status.CREATED)
                .entity(authService.signup(request))
                .build();
    }
}