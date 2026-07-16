package me.msaproject.fleetservice;

import io.helidon.microprofile.testing.junit5.HelidonTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import me.msaproject.fleetservice.utils.GeoUtils;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
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
    void fleetApiDoesNotExposeGeneratedGreetingRoutes() {
        try (Response response = target.path("simple-greet").request().get()) {
            assertThat(response.getStatus(), is(404));
        }
    }

    @Test
    void geoDistanceIsZeroForTheSamePoint() {
        double distance = GeoUtils.distanceMeters(44.4949, 11.3426, 44.4949, 11.3426);
        assertThat(distance, is(closeTo(0.0, 0.001)));
    }
}
