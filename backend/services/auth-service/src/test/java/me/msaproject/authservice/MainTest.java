package me.msaproject.authservice;

import io.helidon.microprofile.testing.junit5.HelidonTest;
import io.helidon.microprofile.testing.junit5.AddConfig;
import io.helidon.microprofile.testing.junit5.AddBean;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import me.msaproject.authservice.db.InMemoryUsersRepository;
import me.msaproject.authservice.mail.CapturingPasswordMailer;

import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

@HelidonTest
@AddBean(InMemoryUsersRepository.class)
@AddBean(CapturingPasswordMailer.class)
@AddConfig(key = "jwt.secret", value = "test-only-secret-that-is-at-least-thirty-two-bytes")
@AddConfig(key = "jwt.issuer", value = "auth-service")
@AddConfig(key = "auth-db.password", value = "test-only")
class MainTest {

    @Inject
    WebTarget target;

    @Inject
    CapturingPasswordMailer passwordMailer;

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

    @Test
    void userCanLogInWithEmailResetPasswordAndChangeIt() throws Exception {
        String suffix = UUID.randomUUID().toString();
        String username = "user-" + suffix;
        String email = suffix + "@example.test";
        String signup = """
                {"username":"%s","password":"initial-secret","email":"%s"}
                """.formatted(username, email);

        try (Response response = target.path("auth/signup").request().post(Entity.json(signup))) {
            assertThat(response.getStatus(), is(201));
        }

        String emailLogin = """
                {"username":"%s","password":"initial-secret"}
                """.formatted(email);
        try (Response response = target.path("auth/login").request().post(Entity.json(emailLogin))) {
            assertThat(response.getStatus(), is(200));
        }

        try (Response response = target.path("auth/password/forgot").request()
                .post(Entity.json("{\"email\":\"" + email + "\"}"))) {
            assertThat(response.getStatus(), is(202));
            assertThat(passwordMailer.recipient(), is(email));
            assertThat(passwordMailer.temporaryPassword().length(), is(14));
        }

        String temporaryLogin = """
                {"username":"%s","password":"%s"}
                """.formatted(email, passwordMailer.temporaryPassword());
        String token;
        try (Response response = target.path("auth/login").request().post(Entity.json(temporaryLogin))) {
            assertThat(response.getStatus(), is(200));
            JsonNode body = new ObjectMapper().readTree(response.readEntity(String.class));
            token = body.get("token").asText();
        }

        String change = """
                {"currentPassword":"%s","newPassword":"new-secret-123"}
                """.formatted(passwordMailer.temporaryPassword());
        try (Response response = target.path("auth/password/change").request()
                .header("Authorization", "Bearer " + token)
                .post(Entity.json(change))) {
            assertThat(response.getStatus(), is(204));
        }

        String changedLogin = """
                {"username":"%s","password":"new-secret-123"}
                """.formatted(username);
        try (Response response = target.path("auth/login").request().post(Entity.json(changedLogin))) {
            assertThat(response.getStatus(), is(200));
        }
    }
}
