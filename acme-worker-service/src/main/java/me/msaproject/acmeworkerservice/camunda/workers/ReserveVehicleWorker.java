package me.msaproject.acmeworkerservice.camunda.workers;

import io.camunda.client.CamundaClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

public class ReserveVehicleWorker implements AcmeWorker {

    private static final Logger LOG = LoggerFactory.getLogger(ReserveVehicleWorker.class);

    private static final String ACME_BASE_URL = "http://acme-mobility-service.acme:8080";

    private static final String ERROR_CODE = "VEHICLE_RESERVATION_FAILED";

    private final CamundaClient client;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public ReserveVehicleWorker(CamundaClient client) {
        this.client = client;
    }

    @Override
    public void open() {
        LOG.info("Opening worker: reserve-vehicle");

        client.newWorker()
                .jobType("reserve-vehicle")
                .handler((jobClient, job) -> {
                    Map<String, Object> variables = job.getVariablesAsMap();

                    String reservationId = String.valueOf(variables.get("reservationId"));

                    try {
                        LOG.info("Confirming reservation: reservationId={}", reservationId);

                        reserveVehicle(reservationId);

                        jobClient.newCompleteCommand(job.getKey())
                                .send()
                                .join();

                        LOG.info("Reservation confirmed: reservationId={}", reservationId);

                    } catch (Exception e) {
                        LOG.error("Vehicle reservation failed: reservationId={}", reservationId, e);

                        jobClient.newThrowErrorCommand(job.getKey())
                                .errorCode(ERROR_CODE)
                                .errorMessage(e.getMessage())
                                .send()
                                .join();
                    }
                })
                .open();
    }

    private void reserveVehicle(String reservationId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ACME_BASE_URL + "/reservations"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        LOG.info("Reserve reservation response: statusCode={}, body={}",
                response.statusCode(), response.body());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException(
                    "Failed to reserve vehicle for reservation " + reservationId +
                            ": HTTP " + response.statusCode()
            );
        }
    }
}