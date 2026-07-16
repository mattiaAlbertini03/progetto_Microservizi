package me.msaproject.acmemobilityservice;

import io.helidon.microprofile.testing.junit5.HelidonTest;
import io.helidon.microprofile.testing.junit5.AddConfig;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
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

    @Test
    void reservationNotificationEndpointAcceptsEveryEnrichedBpmnPayload() {
        String[] payloads = {
                """
                {
                  "reservationId":"reservation-1",
                  "status":"DEPOSIT_HELD",
                  "depositAmount":10,
                  "currency":"EUR",
                  "message":"Deposit held"
                }
                """,
                """
                {
                  "reservationId":"reservation-1",
                  "status":"LOW_BATTERY_PENALTY_APPLIED",
                  "batteryPct":12.5,
                  "penaltyPercentage":10,
                  "baseAmount":8.50,
                  "finalAmount":9.35,
                  "currency":"EUR",
                  "message":"Low-battery penalty applied"
                }
                """,
                """
                {
                  "reservationId":"reservation-1",
                  "status":"DEPOSIT_RELEASED",
                  "depositAmount":10,
                  "currency":"EUR",
                  "message":"Deposit released"
                }
                """,
                """
                {
                  "reservationId":"reservation-1",
                  "status":"RETURN_CONFIRMED",
                  "paymentAmount":9.35,
                  "currency":"EUR",
                  "message":"Return confirmed"
                }
                """
        };

        for (String payload : payloads) {
            try (Response response = target
                    .path("reservations/reservation-1/events")
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .post(Entity.json(payload))) {
                assertThat(response.getStatus(), is(200));
            }
        }
    }
}
