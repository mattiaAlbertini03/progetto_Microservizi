package me.msaproject.acmeworkerservice.camunda.workers;

import io.camunda.client.CamundaClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ReserveVehicleWorker implements AcmeWorker {

    private static final Logger LOG =
            LoggerFactory.getLogger(ReserveVehicleWorker.class);

    private static final String ACME_BASE_URL =
            "http://acme-mobility-service.acme:8080";

    private static final String ERROR_CODE =
            "VEHICLE_RESERVATION_FAILED";

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
                    String reservationId = String.valueOf(
                            job.getVariablesAsMap().get("reservationId")
                    );
                    String vehicleId = String.valueOf(
                            job.getVariablesAsMap().get("vehicleId")
                    );

                    try {
                        LOG.info("Reserving vehicle: reservationId={}, vehicleId={}",
                                reservationId, vehicleId);

                        reserveVehicle(vehicleId);
                        publishReservationEvent(
                                reservationId,
                                "RESERVED",
                                "Veicolo prenotato con successo"
                        );

                        jobClient.newCompleteCommand(job.getKey())
                                .send()
                                .join();

                        LOG.info("Vehicle reserved: reservationId={}, vehicleId={}",
                                reservationId, vehicleId);

                    } catch (Exception e) {
                        LOG.error("Vehicle reservation failed: reservationId={}, vehicleId={}",
                                reservationId, vehicleId, e);

                        publishFailureEventSafely(reservationId, e.getMessage());

                        jobClient.newThrowErrorCommand(job.getKey())
                                .errorCode(ERROR_CODE)
                                .errorMessage(e.getMessage())
                                .send()
                                .join();
                    }
                })
                .open();
    }

    private void reserveVehicle(String vehicleId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ACME_BASE_URL + "/vehicles/" + vehicleId + "/reserve"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        LOG.info("Reserve vehicle response: statusCode={}, body={}",
                response.statusCode(), response.body());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException(
                    "Failed to reserve vehicle " + vehicleId +
                            ": HTTP " + response.statusCode()
            );
        }
    }

    private void publishReservationEvent(
            String reservationId,
            String status,
            String message
    ) throws Exception {
        String json = """
                {
                  "reservationId": "%s",
                  "status": "%s",
                  "message": "%s"
                }
                """.formatted(reservationId, status, message);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                        ACME_BASE_URL + "/reservations/" + reservationId + "/events"
                ))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        LOG.info("Reservation event response: statusCode={}, body={}",
                response.statusCode(), response.body());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException(
                    "Failed to publish reservation event " + reservationId +
                            ": HTTP " + response.statusCode()
            );
        }
    }

    private void publishFailureEventSafely(String reservationId, String errorMessage) {
        try {
            publishReservationEvent(
                    reservationId,
                    "FAILED",
                    "Prenotazione fallita: " + errorMessage
            );
        } catch (Exception eventException) {
            LOG.warn("Could not publish failure event for reservationId={}",
                    reservationId, eventException);
        }
    }
}