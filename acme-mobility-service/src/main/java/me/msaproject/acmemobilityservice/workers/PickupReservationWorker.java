package me.msaproject.acmemobilityservice.workers;

import io.camunda.client.CamundaClient;
import me.msaproject.acmemobilityservice.client.ReservationClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class PickupReservationWorker implements AcmeWorker {

    private static final Logger LOG =
            LoggerFactory.getLogger(PickupReservationWorker.class);

    private static final String ERROR_CODE = "PICKUP_RESERVATION_FAILED";

    private final CamundaClient client;
    private final ReservationClient reservationClient;

    public PickupReservationWorker(
            CamundaClient client,
            ReservationClient reservationClient
    ) {
        this.client = client;
        this.reservationClient = reservationClient;
    }

    @Override
    public void open() {
        LOG.info("Opening worker: pickup-reservation");

        client.newWorker()
                .jobType("pickup-reservation")
                .handler((jobClient, job) -> {
                    Map<String, Object> variables = job.getVariablesAsMap();

                    String reservationId =
                            String.valueOf(variables.get("reservationId"));

                    try {
                        LOG.info("Picking up reservation: reservationId={}", reservationId);

                        reservationClient.pickupReservation(reservationId);

                        jobClient.newCompleteCommand(job.getKey())
                                .send()
                                .join();

                        LOG.info("Reservation picked up: reservationId={}", reservationId);

                    } catch (Exception e) {
                        LOG.error("Reservation pickup failed: reservationId={}", reservationId, e);

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