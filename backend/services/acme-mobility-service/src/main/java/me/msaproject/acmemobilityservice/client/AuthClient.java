package me.msaproject.acmemobilityservice.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import me.msaproject.acmemobilityservice.dto.auth.AuthUserResponse;
import me.msaproject.acmemobilityservice.dto.auth.LoginRequest;
import me.msaproject.acmemobilityservice.dto.auth.LoginResponse;
import me.msaproject.acmemobilityservice.dto.auth.SignupRequest;
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
}