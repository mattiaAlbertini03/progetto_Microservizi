package me.msaproject.acmemobilityservice.workers;

import io.camunda.client.CamundaClient;
import me.msaproject.acmemobilityservice.client.ReservationClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class HoldReservationWorker implements AcmeWorker {

    private static final Logger LOG =
            LoggerFactory.getLogger(HoldReservationWorker.class);

    private static final String ERROR_CODE = "HOLD_RESERVATION_FAILED";

    private final CamundaClient client;
    private final ReservationClient reservationClient;

    public HoldReservationWorker(
            CamundaClient client,
            ReservationClient reservationClient
    ) {
        this.client = client;
        this.reservationClient = reservationClient;
    }

    @Override
    public void open() {
        LOG.info("Opening worker: hold-reservation");

        client.newWorker()
                .jobType("hold-reservation")
                .handler((jobClient, job) -> {
                    Map<String, Object> variables = job.getVariablesAsMap();

                    String reservationId =
                            String.valueOf(variables.get("reservationId"));

                    try {
                        LOG.info("Holding reservation: reservationId={}", reservationId);

                        reservationClient.holdReservation(reservationId);

                        jobClient.newCompleteCommand(job.getKey())
                                .send()
                                .join();

                        LOG.info("Reservation held: reservationId={}", reservationId);

                    } catch (Exception e) {
                        LOG.error("Reservation hold failed: reservationId={}", reservationId, e);

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