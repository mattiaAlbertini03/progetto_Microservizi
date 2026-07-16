package me.msaproject.acmeworkerservice.workers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.client.CamundaClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

public class CalculateChargeWorker implements AcmeWorker {

        private static final Logger LOG = LoggerFactory.getLogger(CalculateChargeWorker.class);

        private static final String RESERVATION_SERVICE_URL = "http://reservation-service.acme:8080";
        private static final String ERROR_CODE = "CALCULATE_CHARGE_FAILED";

        private final CamundaClient client;
        private final HttpClient httpClient = HttpClient.newHttpClient();
        private final ObjectMapper mapper = new ObjectMapper();

        public CalculateChargeWorker(CamundaClient client) {
                this.client = client;
        }

        @Override
        public void open() {
                LOG.info("Opening worker: calculate-charge");

                client.newWorker()
                                .jobType("calculate-charge")
                                .handler((jobClient, job) -> {
                                        Map<String, Object> variables = job.getVariablesAsMap();

                                        String reservationId = "unknown";

                                        try {
                                                ReservationDto reservation = mapper.convertValue(
                                                                variables.get("reservation"),
                                                                ReservationDto.class);

                                                if (reservation == null || reservation.reservationId() == null
                                                                || reservation.reservationId().isBlank()) {
                                                        throw new IllegalArgumentException(
                                                                        "Missing reservation.reservationId process variable");
                                                }

                                                reservationId = reservation.reservationId();
                                                LOG.info("Calculating charge: reservationId={}", reservationId);

                                                ChargeCalculator.Result result = calculateCharge(reservationId, variables);

                                                jobClient.newCompleteCommand(job.getKey())
                                                                .variables(Map.of(
                                                                                "baseAmount", result.baseAmount(),
                                                                                "finalAmount", result.finalAmount(),
                                                                                "batteryPct", result.batteryPct(),
                                                                                "batteryPenaltyApplied",
                                                                                result.batteryPenaltyApplied()))
                                                                .send()
                                                                .join();

                                                LOG.info(
                                                                "Charge calculated: reservationId={}, baseAmount={}, finalAmount={}, batteryPct={}, penalty={}",
                                                                reservationId,
                                                                result.baseAmount(),
                                                                result.finalAmount(),
                                                                result.batteryPct(),
                                                                result.batteryPenaltyApplied());

                                        } catch (Exception e) {
                                                LOG.error("Charge calculation failed: reservationId={}", reservationId,
                                                                e);

                                                jobClient.newThrowErrorCommand(job.getKey())
                                                                .errorCode(ERROR_CODE)
                                                                .errorMessage(e.getMessage())
                                                                .send()
                                                                .join();
                                        }
                                })
                                .open();
        }

        private ChargeCalculator.Result calculateCharge(
                        String reservationId,
                        Map<String, Object> variables) throws Exception {

                getReservation(reservationId);

                int minutesUsed = getRequiredIntVariable(variables, "minutesUsed");
                double kilometers = getRequiredDoubleVariable(variables, "kilometers");
                double batteryPct = getRequiredDoubleVariable(variables, "batteryPct");

                return ChargeCalculator.calculate(minutesUsed, kilometers, batteryPct);
        }

        private ReservationDto getReservation(String reservationId) throws Exception {
                HttpRequest request = HttpRequest.newBuilder()
                                .uri(URI.create(
                                                RESERVATION_SERVICE_URL +
                                                                "/reservations/" +
                                                                reservationId))
                                .GET()
                                .build();

                HttpResponse<String> response = httpClient.send(
                                request,
                                HttpResponse.BodyHandlers.ofString());

                LOG.info("Reservation response: statusCode={}, body={}",
                                response.statusCode(), response.body());

                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        throw new IllegalStateException(
                                        "Failed to get reservation " +
                                                        reservationId +
                                                        ": HTTP " +
                                                        response.statusCode());
                }

                return mapper.readValue(response.body(), ReservationDto.class);
        }

        private int getRequiredIntVariable(Map<String, Object> variables, String key) {
                Object value = variables.get(key);

                if (!(value instanceof Number number)) {
                        throw new IllegalArgumentException("Missing numeric process variable: " + key);
                }

                return number.intValue();
        }

        private double getRequiredDoubleVariable(Map<String, Object> variables, String key) {
                Object value = variables.get(key);

                if (!(value instanceof Number number)) {
                        throw new IllegalArgumentException("Missing numeric process variable: " + key);
                }

                return number.doubleValue();
        }

        private record ReservationDto(
                        String reservationId,
                        String userId,
                        String vehicleId,
                        String stationId,
                        String status,
                        long createdAtEpochMs,
                        long scheduledStartEpochMs,
                        long pickupDeadlineEpochMs) {
        }

}
