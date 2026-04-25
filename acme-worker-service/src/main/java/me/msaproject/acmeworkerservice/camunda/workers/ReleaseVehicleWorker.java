package me.msaproject.acmeworkerservice.camunda.workers;

import io.camunda.client.CamundaClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReleaseVehicleWorker implements AcmeWorker {

    private static final Logger LOG =
            LoggerFactory.getLogger(ReleaseVehicleWorker.class);

    private final CamundaClient client;

    public ReleaseVehicleWorker(CamundaClient client) {
        this.client = client;
    }

    @Override
    public void open() {
        LOG.info("Opening worker: release-vehicle");

        client.newWorker()
                .jobType("release-vehicle")
                .handler((jobClient, job) -> {
                    String reservationId = String.valueOf(
                            job.getVariablesAsMap().get("reservationId")
                    );
                    String vehicleId = String.valueOf(
                            job.getVariablesAsMap().get("vehicleId")
                    );

                    LOG.info("Releasing vehicle: reservationId={}, vehicleId={}",
                            reservationId, vehicleId);

                    // TODO: POST /vehicles/{vehicleId}/release

                    jobClient.newCompleteCommand(job.getKey())
                            .send()
                            .join();

                    LOG.info("Vehicle released: reservationId={}, vehicleId={}",
                            reservationId, vehicleId);
                })
                .open();
    }
}