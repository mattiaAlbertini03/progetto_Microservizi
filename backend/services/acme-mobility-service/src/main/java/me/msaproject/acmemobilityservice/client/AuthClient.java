package me.msaproject.acmemobilityservice.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.WebApplicationException;
import me.msaproject.acmemobilityservice.dto.auth.AuthUserResponse;
import me.msaproject.acmemobilityservice.dto.auth.LoginRequest;
import me.msaproject.acmemobilityservice.dto.auth.LoginResponse;
import me.msaproject.acmemobilityservice.dto.auth.SignupRequest;
import me.msaproject.acmemobilityservice.dto.auth.ChangePasswordRequest;
import me.msaproject.acmemobilityservice.dto.auth.ForgotPasswordRequest;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class AuthClient {

    private final Client client = ClientBuilder.newClient();
    private final String authServiceUrl;

    @Inject
    public AuthClient(
            @ConfigProperty(
                    name = "auth-service.url",
                    defaultValue = "http://auth-service.acme:8080"
            )
            String authServiceUrl
    ) {
        this.authServiceUrl = authServiceUrl;
    }

    public LoginResponse login(LoginRequest request) {
        return client
                .target(authServiceUrl)
                .path("auth")
                .path("login")
                .request()
                .post(Entity.json(request), LoginResponse.class);
    }

    public AuthUserResponse signup(SignupRequest request) {
        return client
                .target(authServiceUrl)
                .path("auth")
                .path("signup")
                .request()
                .post(Entity.json(request), AuthUserResponse.class);
    }

    public void forgotPassword(ForgotPasswordRequest request) {
        try (Response response = client.target(authServiceUrl)
                .path("auth/password/forgot")
                .request()
                .post(Entity.json(request))) {
            requireSuccess(response);
        }
    }

    public void changePassword(String authorizationHeader, ChangePasswordRequest request) {
        try (Response response = client.target(authServiceUrl)
                .path("auth/password/change")
                .request()
                .header("Authorization", authorizationHeader)
                .post(Entity.json(request))) {
            requireSuccess(response);
        }
    }

    private void requireSuccess(Response response) {
        if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
            String message = response.hasEntity() ? response.readEntity(String.class) : "Authentication request failed";
            throw new WebApplicationException(message, response.getStatus());
        }
    }
}
