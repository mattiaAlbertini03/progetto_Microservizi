package me.msaproject.acmemobilityservice.auth;

import io.camunda.client.CamundaClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Map;

@ApplicationScoped
public class AuthService {

    private static final String DEMO_USERNAME = "demo";
    private static final String DEMO_PASSWORD = "demo";

    @Inject
    CamundaClient camundaClient;

    public LoginResponse login(LoginRequest request) {
        if (request == null ||
                !DEMO_USERNAME.equals(request.username) ||
                !DEMO_PASSWORD.equals(request.password)) {
            throw new RuntimeException("Invalid demo credentials");
        }

        String userId = "user-demo-001";

        camundaClient
                .newCreateInstanceCommand()
                .bpmnProcessId("process")
                .latestVersion()
                .variables(Map.of(
                        "userId", userId,
                        "bankLoginPayload", Map.of(
                                "username", request.username,
                                "account", 1000,
                                "deposit", 500)))
                .send()
                .join();

        return new LoginResponse(
                true,
                userId,
                request.username);
    }
}