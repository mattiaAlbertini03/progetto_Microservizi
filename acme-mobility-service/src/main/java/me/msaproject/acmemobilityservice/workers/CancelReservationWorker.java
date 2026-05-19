package me.msaproject.acmemobilityservice.workers;

import io.camunda.client.CamundaClient;
import me.msaproject.acmemobilityservice.client.ReservationClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class CancelReservationWorker implements AcmeWorker {

    private static final Logger LOG =
            LoggerFactory.getLogger(CancelReservationWorker.class);

    private static final String ERROR_CODE = "CANCEL_RESERVATION_FAILED";

    private final CamundaClient client;
    private final ReservationClient reservationClient;

    public CancelReservationWorker(
            CamundaClient client,
            ReservationClient reservationClient
    ) {
        this.client = client;
        this.reservationClient = reservationClient;
    }

    @Override
    public void open() {
        LOG.info("Opening worker: cancel-reservation");

        client.newWorker()
                .jobType("cancel-reservation")
                .handler((jobClient, job) -> {
                    Map<String, Object> variables = job.getVariablesAsMap();

                    String reservationId =
                            String.valueOf(variables.get("reservationId"));

                    try {
                        LOG.info("Cancelling reservation: reservationId={}", reservationId);

                        reservationClient.cancelReservation(reservationId);

                        jobClient.newCompleteCommand(job.getKey())
                                .send()
                                .join();

                        LOG.info("Reservation cancelled: reservationId={}", reservationId);

                    } catch (Exception e) {
                        LOG.error("Reservation cancellation failed: reservationId={}", reservationId, e);

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