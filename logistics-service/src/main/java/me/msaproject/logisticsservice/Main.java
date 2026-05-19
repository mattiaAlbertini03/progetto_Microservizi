package me.msaproject.logisticsservice;

import me.msaproject.logisticsservice.db.JsonVehicleRepository;
import me.msaproject.logisticsservice.logistics.LogisticsService;
import me.msaproject.logisticsservice.workers.ChargingMonitorWorker;
import me.msaproject.logisticsservice.workers.VehicleChargingWorker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws InterruptedException {
        LOG.info("Starting acme-worker-service...");

        LogisticsService logisticsService = new LogisticsService(
                new JsonVehicleRepository());

        new Thread(new ChargingMonitorWorker(logisticsService)).start();
        new Thread(new VehicleChargingWorker(logisticsService)).start();

        LOG.info("All workers started");

        Thread.currentThread().join();
    }
}