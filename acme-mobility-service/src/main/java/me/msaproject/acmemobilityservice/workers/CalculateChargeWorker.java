package me.msaproject.acmemobilityservice.workers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.client.CamundaClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

public class CalculateChargeWorker implements AcmeWorker {

    private static final Logger LOG = LoggerFactory.getLogger(CalculateChargeWorker.class);

    private static final String RESERVATION_SERVICE_URL = "http://reservation-service.acme:8080";
    private static final String LOGISTICS_SERVICE_URL = "http://logistics-service.acme:8080";

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

                    String reservationId = String.valueOf(variables.get("reservationId"));

                    try {
                        LOG.info("Calculating charge: reservationId={}", reservationId);

                        ChargeResult result = calculateCharge(reservationId, variables);

                        jobClient.newCompleteCommand(job.getKey())
                                .variables(Map.of(
                                        "baseAmount", result.baseAmount(),
                                        "finalAmount", result.finalAmount(),
                                        "batteryPct", result.batteryPct(),
                                        "batteryPenaltyApplied", result.batteryPenaltyApplied()
                                ))
                                .send()
                                .join();

                        LOG.info(
                                "Charge calculated: reservationId={}, baseAmount={}, finalAmount={}, batteryPct={}, penalty={}",
                                reservationId,
                                result.baseAmount(),
                                result.finalAmount(),
                                result.batteryPct(),
                                result.batteryPenaltyApplied()
                        );

                    } catch (Exception e) {
                        LOG.error("Charge calculation failed: reservationId={}", reservationId, e);

                        jobClient.newThrowErrorCommand(job.getKey())
                                .errorCode(ERROR_CODE)
                                .errorMessage(e.getMessage())
                                .send()
                                .join();
                    }
                })
                .open();
    }

    private ChargeResult calculateCharge(
            String reservationId,
            Map<String, Object> variables
    ) throws Exception {

        ReservationDto reservation = getReservation(reservationId);
        VehicleDto vehicle = getVehicle(reservation.vehicleId());

        int minutesUsed = getIntVariable(variables, "minutesUsed", 0);
        double kilometers = getDoubleVariable(variables, "kilometers", 0.0);

        BigDecimal baseAmount = calculateBaseAmount(minutesUsed, kilometers);

        boolean batteryPenaltyApplied = vehicle.batteryPct() < 15;

        BigDecimal finalAmount = batteryPenaltyApplied
                ? baseAmount.multiply(BigDecimal.valueOf(1.10))
                : baseAmount;

        return new ChargeResult(
                baseAmount.setScale(2, RoundingMode.HALF_UP),
                finalAmount.setScale(2, RoundingMode.HALF_UP),
                vehicle.batteryPct(),
                batteryPenaltyApplied
        );
    }

    private ReservationDto getReservation(String reservationId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                        RESERVATION_SERVICE_URL +
                                "/reservations/" +
                                reservationId
                ))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        LOG.info("Reservation response: statusCode={}, body={}",
                response.statusCode(), response.body());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException(
                    "Failed to get reservation " +
                            reservationId +
                            ": HTTP " +
                            response.statusCode()
            );
        }

        return mapper.readValue(response.body(), ReservationDto.class);
    }

    private VehicleDto getVehicle(String vehicleId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                        LOGISTICS_SERVICE_URL +
                                "/vehicles/" +
                                vehicleId
                ))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        LOG.info("Vehicle response: statusCode={}, body={}",
                response.statusCode(), response.body());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException(
                    "Failed to get vehicle " +
                            vehicleId +
                            ": HTTP " +
                            response.statusCode()
            );
        }

        return mapper.readValue(response.body(), VehicleDto.class);
    }

    private BigDecimal calculateBaseAmount(int minutesUsed, double kilometers) {
        BigDecimal unlockFee = BigDecimal.valueOf(1.00);

        BigDecimal timeCost = BigDecimal.valueOf(minutesUsed)
                .multiply(BigDecimal.valueOf(0.20));

        BigDecimal distanceCost = BigDecimal.valueOf(kilometers)
                .multiply(BigDecimal.valueOf(0.50));

        return unlockFee.add(timeCost).add(distanceCost);
    }

    private int getIntVariable(
            Map<String, Object> variables,
            String key,
            int defaultValue
    ) {
        Object value = variables.get(key);

        if (value == null) {
            return defaultValue;
        }

        return ((Number) value).intValue();
    }

    private double getDoubleVariable(
            Map<String, Object> variables,
            String key,
            double defaultValue
    ) {
        Object value = variables.get(key);

        if (value == null) {
            return defaultValue;
        }

        return ((Number) value).doubleValue();
    }

    private record ReservationDto(
            String id,
            String userId,
            String vehicleId,
            String stationId,
            String status
    ) {}

    private record VehicleDto(
            String id,
            String type,
            Double lat,
            Double lon,
            String status,
            int batteryPct,
            String qrCode
    ) {}

    private record ChargeResult(
            BigDecimal baseAmount,
            BigDecimal finalAmount,
            int batteryPct,
            boolean batteryPenaltyApplied
    ) {}
}