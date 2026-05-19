package me.msaproject.acmeworkerservice.camunda.workers;

import io.camunda.client.CamundaClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

public class WithdrawVehicleWorker implements AcmeWorker {

        private static final Logger LOG = LoggerFactory.getLogger(WithdrawVehicleWorker.class);

        private static final String ACME_BASE_URL = "http://acme-mobility-service.acme:8080";

        private static final String ERROR_CODE = "VEHICLE_WITHDRAWAL_FAILED";

        private final CamundaClient client;
        private final HttpClient httpClient = HttpClient.newHttpClient();

        public WithdrawVehicleWorker(CamundaClient client) {
                this.client = client;
        }

        @Override
        public void open() {
                LOG.info("Opening worker: withdraw-vehicle");

                client.newWorker()
                                .jobType("withdraw-vehicle")
                                .handler((jobClient, job) -> {
                                        Map<String, Object> variables = job.getVariablesAsMap();

                                        String vehicleId = String.valueOf(variables.get("vehicleId"));
                                        String vehicleState = String.valueOf(
                                                        variables.getOrDefault("vehicleState", "RESERVED"));
                                        String reservationId = String.valueOf(variables.get("reservationId"));

                                        try {
                                                LOG.info("Updating vehicle state: vehicleId={}, state={}",
                                                                vehicleId, vehicleState);

                                                withdrawVehicle(vehicleId);

                                                jobClient.newCompleteCommand(job.getKey())
                                                                .variables(Map.of(
                                                                                "vehicleId", vehicleId,
                                                                                "vehicleState", vehicleState,
                                                                                "vehicleStateUpdated", true))
                                                                .send()
                                                                .join();

                                                LOG.info("Completed vehicle state update: vehicleId={}, state={}",
                                                                vehicleId, vehicleState);
                                        } catch (Exception e) {
                                                LOG.error("Vehicle withdrawal failed: reservationId={}, vehicleId={}",
                                                                reservationId, vehicleId, e);

                                                jobClient.newThrowErrorCommand(job.getKey())
                                                                .errorCode(ERROR_CODE)
                                                                .errorMessage(e.getMessage())
                                                                .send()
                                                                .join();
                                        }
                                }).open();

        }

        private void withdrawVehicle(String vehicleId) throws Exception {
                HttpRequest request = HttpRequest.newBuilder()
                                .uri(URI.create(ACME_BASE_URL + "/vehicles/" + vehicleId + "/withdraw"))
                                .POST(HttpRequest.BodyPublishers.noBody())
                                .build();

                HttpResponse<String> response = httpClient.send(
                                request,
                                HttpResponse.BodyHandlers.ofString());

                LOG.info("Reserve vehicle response: statusCode={}, body={}",
                                response.statusCode(), response.body());

                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        throw new IllegalStateException(
                                        "Failed to reserve vehicle " + vehicleId +
                                                        ": HTTP " + response.statusCode());
                }
        }
}