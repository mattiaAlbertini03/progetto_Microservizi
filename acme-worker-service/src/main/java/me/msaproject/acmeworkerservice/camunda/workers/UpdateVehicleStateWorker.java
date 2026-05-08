package me.msaproject.acmeworkerservice.camunda.workers;

import io.camunda.client.CamundaClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class UpdateVehicleStateWorker implements AcmeWorker {

    private static final Logger LOG =
            LoggerFactory.getLogger(UpdateVehicleStateWorker.class);

    private final CamundaClient client;

    public UpdateVehicleStateWorker(CamundaClient client) {
        this.client = client;
    }

    @Override
    public void open() {
        LOG.info("Opening worker: update-vehicle-state");

        client.newWorker()
                .jobType("update-vehicle-state")
                .handler((jobClient, job) -> {
                    Map<String, Object> variables = job.getVariablesAsMap();

                    String vehicleId = String.valueOf(variables.get("vehicleId"));
                    String vehicleState = String.valueOf(
                            variables.getOrDefault("vehicleState", "RESERVED")
                    );

                    LOG.info("Updating vehicle state: vehicleId={}, state={}",
                            vehicleId, vehicleState);

                    // TODO: chiamare vehicle-service / repository reale
                    // Esempio logico:
                    // vehicleClient.updateState(vehicleId, vehicleState);

                    jobClient.newCompleteCommand(job.getKey())
                            .variables(Map.of(
                                    "vehicleId", vehicleId,
                                    "vehicleState", vehicleState,
                                    "vehicleStateUpdated", true
                            ))
                            .send()
                            .join();

                    LOG.info("Completed vehicle state update: vehicleId={}, state={}",
                            vehicleId, vehicleState);
                })
                .open();
    }
}