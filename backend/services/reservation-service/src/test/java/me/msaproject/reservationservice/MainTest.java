package me.msaproject.reservationservice;

import io.helidon.microprofile.testing.junit5.AddBean;
import io.helidon.microprofile.testing.junit5.AddConfig;
import io.helidon.microprofile.testing.junit5.HelidonTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import me.msaproject.reservationservice.db.InMemoryReservationRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

@HelidonTest
@AddBean(InMemoryReservationRepository.class)
@AddConfig(key = "reservation-db.password", value = "test-only")
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
    void reservationCanBeCreatedAndListedForItsUser() {
        String userId = "user-" + UUID.randomUUID();
        long scheduledStart = Instant.now().plusSeconds(60).toEpochMilli();
        String request = """
                {
                  "userId":"%s",
                  "vehicleId":"vehicle-1",
                  "stationId":"station-1",
                  "scheduledStartEpochMs":%d
                }
                """.formatted(userId, scheduledStart);

        String reservationId;
        try (Response response = target.path("reservations")
                .request()
                .post(Entity.json(request))) {
            assertThat(response.getStatus(), is(201));
            String body = response.readEntity(String.class);
            assertThat(body, containsString("\"status\":\"PENDING\""));
            reservationId = extractJsonString(body, "reservationId");
        }

        try (Response response = target.path("reservations/" + reservationId)
                .request()
                .get()) {
            assertThat(response.getStatus(), is(200));
            assertThat(response.readEntity(String.class), containsString(userId));
        }

        try (Response response = target.path("reservations")
                .queryParam("userId", userId)
                .request()
                .get()) {
            assertThat(response.getStatus(), is(200));
            assertThat(response.readEntity(String.class), containsString(reservationId));
        }
    }

    @Test
    void listingReservationsRequiresAUserId() {
        try (Response response = target.path("reservations").request().get()) {
            assertThat(response.getStatus(), is(400));
        }
    }

    private static String extractJsonString(String json, String field) {
        String prefix = "\"" + field + "\":\"";
        int start = json.indexOf(prefix);
        if (start < 0) {
            throw new AssertionError("Missing JSON field: " + field + " in " + json);
        }
        start += prefix.length();
        int end = json.indexOf('"', start);
        return json.substring(start, end);
    }
}
