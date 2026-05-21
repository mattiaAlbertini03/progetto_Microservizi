package me.msaproject.acmemobilityservice.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import me.msaproject.acmemobilityservice.dto.auth.AuthUserResponse;
import me.msaproject.acmemobilityservice.dto.auth.LoginRequest;
import me.msaproject.acmemobilityservice.dto.auth.SignupRequest;

@ApplicationScoped
public class AuthClient {

    private static final String AUTH_BASE_URL =
            "http://auth-service.acme:8080";

    private final Client client = ClientBuilder.newClient();

    public AuthUserResponse login(LoginRequest request) {
        return client
                .target(AUTH_BASE_URL)
                .path("/auth/login")
                .request()
                .post(Entity.json(request), AuthUserResponse.class);
    }

    public AuthUserResponse signup(SignupRequest request) {
        return client
                .target(AUTH_BASE_URL)
                .path("/auth/signup")
                .request()
                .post(Entity.json(request), AuthUserResponse.class);
    }
}