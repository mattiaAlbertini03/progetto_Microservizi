package me.msaproject.logisticsservice;

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
@AddConfig(key = "logistics.workers.enabled", value = "false")
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
    void vehicleCatalogueContainsConfiguredVehicles() {
        try (Response response = target.path("vehicles").request().get()) {
            assertThat(response.getStatus(), is(200));
            String body = response.readEntity(String.class);
            assertThat(body, containsString("vehicle-1"));
            assertThat(body, containsString("ACME-QR-vehicle-1"));
        }
    }

    @Test
    void vehicleCanBeReadByIdentifier() {
        try (Response response = target.path("vehicles/vehicle-1").request().get()) {
            assertThat(response.getStatus(), is(200));
            assertThat(response.readEntity(String.class), containsString("\"batteryPct\":96"));
        }
    }
}
