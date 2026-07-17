package me.msaproject.acmemobilityservice.api;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.HeaderParam;
import me.msaproject.acmemobilityservice.auth.JwtService;
import me.msaproject.acmemobilityservice.client.AuthClient;
import me.msaproject.acmemobilityservice.dto.auth.LoginRequest;
import me.msaproject.acmemobilityservice.dto.auth.LoginResponse;
import me.msaproject.acmemobilityservice.dto.auth.SignupRequest;
import me.msaproject.acmemobilityservice.dto.auth.ChangePasswordRequest;
import me.msaproject.acmemobilityservice.dto.auth.ForgotPasswordRequest;

@Path("/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthApi {

    @Inject
    AuthClient authClient;

    @Inject
    JwtService jwtService;

    @POST
    @Path("/login")
    public Response login(LoginRequest request) {
        LoginResponse response = authClient.login(request);
        return Response.ok(response).build();
    }

    @POST
    @Path("/signup")
    public Response signup(SignupRequest request) {
        return Response.status(Response.Status.CREATED)
                .entity(authClient.signup(request))
                .build();
    }

    @POST
    @Path("/password/forgot")
    public Response forgotPassword(ForgotPasswordRequest request) {
        authClient.forgotPassword(request);
        return Response.accepted().build();
    }

    @POST
    @Path("/password/change")
    public Response changePassword(
            @HeaderParam("Authorization") String authorizationHeader,
            ChangePasswordRequest request
    ) {
        jwtService.authenticate(authorizationHeader);
        authClient.changePassword(authorizationHeader, request);
        return Response.noContent().build();
    }
}
