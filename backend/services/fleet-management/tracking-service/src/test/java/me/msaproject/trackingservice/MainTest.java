package me.msaproject.trackingservice;

import io.helidon.microprofile.testing.junit5.HelidonTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
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
    void rejectsMissingVehicleId() {
        assertBadRequest("{\"vehicleId\":\"\",\"lat\":44.5,\"lon\":11.3}");
    }

    @Test
    void rejectsInvalidCoordinates() {
        assertBadRequest("{\"vehicleId\":\"vehicle-1\",\"lat\":91,\"lon\":11.3}");
        assertBadRequest("{\"vehicleId\":\"vehicle-1\",\"lat\":44.5,\"lon\":181}");
    }

    private void assertBadRequest(String json) {
        try (Response response = target.path("tracking")
                .request()
                .post(Entity.json(json))) {
            assertThat(response.getStatus(), is(400));
        }
    }
}
