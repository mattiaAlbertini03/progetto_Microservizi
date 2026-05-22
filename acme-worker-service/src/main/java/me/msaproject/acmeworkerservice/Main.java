package me.msaproject.acmeworkerservice;

import io.camunda.client.CamundaClient;
import me.msaproject.acmeworkerservice.camunda.CamundaClientFactory;
import me.msaproject.acmeworkerservice.workers.CalculateChargeWorker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws InterruptedException {
        LOG.info("Starting acme-worker-service...");

        CamundaClient camundaClient = CamundaClientFactory.create();

        camundaClient.newDeployResourceCommand()
                .addResourceFromClasspath("process.bpmn")
                .execute();

        LOG.info("Deployed BPMN.");

        camundaClient.newTopologyRequest().execute();
        LOG.info("Connected to Camunda");

        new CalculateChargeWorker(camundaClient).open();

        LOG.info("All workers started");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOG.info("Closing Camunda client...");
            camundaClient.close();
        }));

        Thread.currentThread().join();
    }
}