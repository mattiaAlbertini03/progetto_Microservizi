package me.msaproject.acmemobilityservice;

import io.helidon.microprofile.testing.junit5.HelidonTest;
import io.helidon.microprofile.testing.junit5.AddConfig;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
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
    void corsPreflightIsAcceptedForApiPaths() {
        try (Response response = target.path("reservations/example/events")
                .request()
                .options()) {
            assertThat(response.getStatus(), is(200));
        }
    }
}
