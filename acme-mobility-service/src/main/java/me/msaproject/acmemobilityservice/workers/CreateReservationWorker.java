package me.msaproject.acmemobilityservice.workers;

import io.camunda.client.CamundaClient;
import me.msaproject.acmemobilityservice.client.ReservationClient;
import me.msaproject.acmemobilityservice.dto.reservation.ReservationRequest;
import me.msaproject.acmemobilityservice.dto.reservation.ReservationResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class CreateReservationWorker implements AcmeWorker {

    private static final Logger LOG =
            LoggerFactory.getLogger(CreateReservationWorker.class);

    private static final String ERROR_CODE = "CREATE_RESERVATION_FAILED";

    private final CamundaClient client;
    private final ReservationClient reservationClient;

    public CreateReservationWorker(
            CamundaClient client,
            ReservationClient reservationClient) {
        this.client = client;
        this.reservationClient = reservationClient;
    }

    @Override
    public void open() {
        LOG.info("Opening worker: create-reservation");

        client.newWorker()
                .jobType("create-reservation")
                .handler((jobClient, job) -> {
                    Map<String, Object> variables = job.getVariablesAsMap();

                    String requestId = String.valueOf(variables.get("requestId"));
                    String userId = String.valueOf(variables.get("userId"));
                    String vehicleId = String.valueOf(variables.get("vehicleId"));
                    String stationId = String.valueOf(variables.get("stationId"));

                    try {
                        LOG.info(
                                "Creating reservation: requestId={}, userId={}, vehicleId={}, stationId={}",
                                requestId,
                                userId,
                                vehicleId,
                                stationId
                        );

                        ReservationResponse reservationResponse =
                                reservationClient.createReservation(new ReservationRequest(
                                        userId,
                                        vehicleId,
                                        stationId
                                ));

                        String reservationId = reservationResponse.reservationId();

                        jobClient.newCompleteCommand(job.getKey())
                                .variables(Map.of(
                                        "requestId", requestId,
                                        "reservationId", reservationId
                                ))
                                .send()
                                .join();

                        LOG.info(
                                "Reservation created: requestId={}, reservationId={}",
                                requestId,
                                reservationId
                        );

                    } catch (Exception e) {
                        LOG.error(
                                "Reservation creation failed: requestId={}, userId={}, vehicleId={}, stationId={}",
                                requestId,
                                userId,
                                vehicleId,
                                stationId,
                                e
                        );

                        jobClient.newThrowErrorCommand(job.getKey())
                                .errorCode(ERROR_CODE)
                                .errorMessage(e.getMessage())
                                .send()
                                .join();
                    }
                })
                .open();
    }
}