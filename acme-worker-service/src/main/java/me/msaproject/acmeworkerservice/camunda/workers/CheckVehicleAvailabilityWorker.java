package me.msaproject.acmeworkerservice.camunda.workers;

import io.camunda.client.CamundaClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CheckVehicleAvailabilityWorker implements AcmeWorker {

    private static final Logger LOG =
            LoggerFactory.getLogger(CheckVehicleAvailabilityWorker.class);

    private final CamundaClient client;

    public CheckVehicleAvailabilityWorker(CamundaClient client) {
        this.client = client;
    }

    @Override
    public void open() {
        LOG.info("Opening worker: check-vehicle-availability");

        client.newWorker()
                .jobType("check-vehicle-availability")
                .handler((jobClient, job) -> {
                    String vehicleId = String.valueOf(
                            job.getVariablesAsMap().get("vehicleId")
                    );

                    LOG.info("Checking availability for vehicleId={}", vehicleId);

                    boolean available = true;

                    jobClient.newCompleteCommand(job.getKey())
                            .variable("available", available)
                            .send()
                            .join();

                    LOG.info("Completed availability check: vehicleId={}, available={}",
                            vehicleId, available);
                })
                .open();
    }
}