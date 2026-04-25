package me.msaproject.acmeworkerservice.camunda.workers;

import io.camunda.client.CamundaClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReserveVehicleWorker implements AcmeWorker {

    private static final Logger LOG =
            LoggerFactory.getLogger(ReserveVehicleWorker.class);

    private final CamundaClient client;

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

                    LOG.info("Reserving vehicle: reservationId={}, vehicleId={}",
                            reservationId, vehicleId);

                    // TODO: qui poi chiamerai acme-mobility-service
                    // POST /vehicles/{vehicleId}/reserve

                    jobClient.newCompleteCommand(job.getKey())
                            .send()
                            .join();

                    LOG.info("Vehicle reserved: reservationId={}, vehicleId={}",
                            reservationId, vehicleId);
                })
                .open();
    }
}