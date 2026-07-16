package me.msaproject.authservice;

import io.helidon.microprofile.testing.junit5.HelidonTest;
import io.helidon.microprofile.testing.junit5.AddConfig;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

@HelidonTest
@AddConfig(key = "jwt.secret", value = "test-only-secret-that-is-at-least-thirty-two-bytes")
@AddConfig(key = "jwt.issuer", value = "auth-service")
class MainTest {

    @Inject
    WebTarget target;

    @Test
    void healthEndpointIsAvailable() {
        try (Response response = target.path("health").request().get()) {
            assertThat(response.getStatus(), is(200));
        }
    }

    @Test
    void userCanSignUpAndLogInButCannotReuseUsername() {
        String suffix = UUID.randomUUID().toString();
        String username = "user-" + suffix;
        String email = suffix + "@example.test";
        String signup = """
                {"username":"%s","password":"secret","email":"%s"}
                """.formatted(username, email);

        try (Response response = target.path("auth/signup")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(signup))) {
            assertThat(response.getStatus(), is(201));
            assertThat(response.readEntity(String.class), containsString(username));
        }

        String login = """
                {"username":"%s","password":"secret"}
                """.formatted(username);

        try (Response response = target.path("auth/login")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(login))) {
            assertThat(response.getStatus(), is(200));
            assertThat(response.readEntity(String.class), containsString("token"));
        }

        try (Response response = target.path("auth/signup")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(signup))) {
            assertThat(response.getStatus(), is(409));
        }
    }
}
