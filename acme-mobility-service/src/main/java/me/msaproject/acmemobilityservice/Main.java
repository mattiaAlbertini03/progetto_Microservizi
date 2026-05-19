package me.msaproject.acmemobilityservice;

import io.camunda.client.CamundaClient;
import me.msaproject.acmemobilityservice.camunda.CamundaClientFactory;
import me.msaproject.acmemobilityservice.client.ReservationClient;
import me.msaproject.acmemobilityservice.workers.CalculateChargeWorker;
import me.msaproject.acmemobilityservice.workers.CancelReservationWorker;
import me.msaproject.acmemobilityservice.workers.CreateReservationWorker;
import me.msaproject.acmemobilityservice.workers.HoldReservationWorker;
import me.msaproject.acmemobilityservice.workers.PickupReservationWorker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws InterruptedException {
        LOG.info("Starting acme-worker-service...");

        CamundaClient camundaClient = CamundaClientFactory.create();
        ReservationClient reservationClient = new ReservationClient();

        camundaClient.newDeployResourceCommand()
                .addResourceFromClasspath("process.bpmn")
                .execute();

        LOG.info("Deployed BPMN.");

        camundaClient.newTopologyRequest().execute();
        LOG.info("Connected to Camunda");

        new CreateReservationWorker(camundaClient, reservationClient).open();
        new HoldReservationWorker(camundaClient, reservationClient).open();
        new CancelReservationWorker(camundaClient, reservationClient).open();
        new PickupReservationWorker(camundaClient, reservationClient).open();
        new CalculateChargeWorker(camundaClient).open();

        LOG.info("All workers started");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOG.info("Closing Camunda client...");
            camundaClient.close();
        }));

        Thread.currentThread().join();
    }
}