package me.msaproject.queryservice;

import io.helidon.microprofile.testing.junit5.AddConfig;
import io.helidon.microprofile.testing.junit5.HelidonTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

@HelidonTest
@AddConfig(key = "kafka.consumer.enabled", value = "false")
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
    void emptyReadModelReturnsAnEmptyCollection() {
        try (Response response = target.path("vehicles").request().get()) {
            assertThat(response.getStatus(), is(200));
            assertThat(response.readEntity(String.class), is("[]"));
        }
    }

    @Test
    void unknownVehicleReturnsStructuredNotFoundResponse() {
        try (Response response = target.path("vehicles/missing").request().get()) {
            assertThat(response.getStatus(), is(404));
            assertThat(response.readEntity(String.class), containsString("vehicle_not_found"));
        }
    }
}
