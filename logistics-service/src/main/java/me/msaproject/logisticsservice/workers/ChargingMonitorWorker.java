package me.msaproject.logisticsservice.workers;

import me.msaproject.logisticsservice.logistics.LogisticsService;
import me.msaproject.logisticsservice.logistics.Vehicle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ChargingMonitorWorker implements Runnable {

    private static final Logger LOG =
            LoggerFactory.getLogger(ChargingMonitorWorker.class);

    private final LogisticsService logisticsService;

    public ChargingMonitorWorker(LogisticsService logisticsService) {
        this.logisticsService = logisticsService;
    }

    @Override
    public void run() {
        while (true) {
            try {
                List<Vehicle> vehicles = logisticsService.getVehiclesNeedingCharge();

                for (Vehicle vehicle : vehicles) {
                    LOG.info("Vehicle needs charging: vehicleId={}, batteryPct={}",
                            vehicle.id(),
                            vehicle.batteryPct()
                    );

                    logisticsService.startCharging(vehicle.id());
                }

                Thread.sleep(30_000);

            } catch (Exception e) {
                LOG.error("Charging monitor failed", e);

                try {
                    Thread.sleep(30_000);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }
}