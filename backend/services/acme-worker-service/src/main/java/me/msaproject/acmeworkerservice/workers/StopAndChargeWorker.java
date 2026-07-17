package me.msaproject.acmeworkerservice.workers;

import io.camunda.client.CamundaClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

public class StopAndChargeWorker implements AcmeWorker {
    private static final Logger LOG = LoggerFactory.getLogger(StopAndChargeWorker.class);
    private static final String FLEET_SERVICE_URL = "http://fleet-service.acme:8080";
    private final CamundaClient client;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public StopAndChargeWorker(CamundaClient client) {
        this.client = client;
    }

    @Override
    public void open() {
        LOG.info("Opening worker: stop-and-charge");
        client.newWorker().jobType("stop-and-charge").handler((jobClient, job) -> {
            try {
                Object rawReservation = job.getVariablesAsMap().get("reservation");
                if (!(rawReservation instanceof Map<?, ?> reservation)) {
                    throw new IllegalArgumentException("Missing reservation process variable");
                }
                String reservationId = String.valueOf(reservation.get("reservationId"));
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(FLEET_SERVICE_URL + "/fleet/" + reservationId + "/recharge-and-resume"))
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new IllegalStateException("Fleet recharge failed: HTTP " + response.statusCode());
                }
                jobClient.newCompleteCommand(job.getKey()).variables(Map.of(
                        "batteryPct", 25,
                        "batterySafetyThresholdBreached", true,
                        "batteryPenaltyApplied", true)).send().join();
            } catch (Exception exception) {
                LOG.error("Safety recharge failed", exception);
                jobClient.newFailCommand(job.getKey())
                        .retries(Math.max(0, job.getRetries() - 1))
                        .errorMessage(exception.getMessage()).send().join();
            }
        }).open();
    }
}
