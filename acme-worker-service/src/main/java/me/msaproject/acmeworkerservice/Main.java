package me.msaproject.acmeworkerservice;

import io.camunda.client.CamundaClient;
import me.msaproject.acmeworkerservice.camunda.CamundaClientFactory;
import me.msaproject.acmeworkerservice.camunda.workers.CheckVehicleAvailabilityWorker;
import me.msaproject.acmeworkerservice.camunda.workers.ReleaseVehicleWorker;
import me.msaproject.acmeworkerservice.camunda.workers.ReserveVehicleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws InterruptedException {
        LOG.info("Starting acme-worker-service...");

        CamundaClient client = CamundaClientFactory.create();

        client.newDeployResourceCommand()
                .addResourceFromClasspath("process.bpmn")
                .execute();

        LOG.info("Deployed BPMN.");

        client.newTopologyRequest().execute();
        LOG.info("Connected to Camunda");

        new CheckVehicleAvailabilityWorker(client).open();
        new ReserveVehicleWorker(client).open();
        new ReleaseVehicleWorker(client).open();

        LOG.info("All workers started");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOG.info("Closing Camunda client...");
            client.close();
        }));

        Thread.currentThread().join();
    }
}