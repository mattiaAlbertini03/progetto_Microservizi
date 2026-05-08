package me.msaproject.acmeworkerservice.camunda.workers;

import io.camunda.client.CamundaClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class CancelReservationWorker implements AcmeWorker {

    private static final Logger LOG =
            LoggerFactory.getLogger(CancelReservationWorker.class);

    private final CamundaClient client;

    public CancelReservationWorker(CamundaClient client) {
        this.client = client;
    }

    @Override
    public void open() {
        LOG.info("Opening worker: cancel-reservation");

        client.newWorker()
                .jobType("cancel-reservation")
                .handler((jobClient, job) -> {
                    Map<String, Object> variables = job.getVariablesAsMap();

                    String reservationId = String.valueOf(variables.get("reservationId"));
                    String vehicleId = String.valueOf(variables.get("vehicleId"));

                    LOG.info("Cancelling reservation: reservationId={}, vehicleId={}",
                            reservationId, vehicleId);

                    // TODO: chiamare reservation-service / repository reale
                    // reservationClient.cancelReservation(reservationId);

                    jobClient.newCompleteCommand(job.getKey())
                            .variables(Map.of(
                                    "reservationId", reservationId,
                                    "vehicleId", vehicleId,
                                    "reservationStatus", "CANCELLED",
                                    "reservationCancelled", true
                            ))
                            .send()
                            .join();

                    LOG.info("Completed reservation cancellation: reservationId={}",
                            reservationId);
                })
                .open();
    }
}