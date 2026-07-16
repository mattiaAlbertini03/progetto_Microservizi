package me.msaproject.simulatorservice;

import io.helidon.microprofile.testing.junit5.HelidonTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

@HelidonTest
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
    void unknownVehicleHasNoActiveSimulation() {
        try (Response response = target.path("simulations/missing").request().get()) {
            assertThat(response.getStatus(), is(200));
            String body = response.readEntity(String.class);
            assertThat(body, containsString("\"vehicleId\":\"missing\""));
            assertThat(body, containsString("\"active\":false"));
        }
    }
}
