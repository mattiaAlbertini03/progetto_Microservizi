package me.msaproject.logisticsservice.workers;

import me.msaproject.logisticsservice.logistics.LogisticsService;
import me.msaproject.logisticsservice.logistics.Vehicle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class VehicleChargingWorker implements Runnable {

    private static final Logger LOG =
            LoggerFactory.getLogger(VehicleChargingWorker.class);

    private final LogisticsService logisticsService;

    public VehicleChargingWorker(LogisticsService logisticsService) {
        this.logisticsService = logisticsService;
    }

    @Override
    public void run() {
        while (true) {
            try {
                List<Vehicle> chargingVehicles =
                        logisticsService.getChargingVehicles();

                for (Vehicle vehicle : chargingVehicles) {
                    logisticsService.increaseBattery(vehicle.id(), 5);

                    LOG.info("Charging vehicle: vehicleId={}, batteryPct={}",
                            vehicle.id(),
                            Math.min(100, vehicle.batteryPct() + 5)
                    );

                    if (vehicle.batteryPct() + 5 >= 100) {
                        logisticsService.stopCharging(vehicle.id());

                        LOG.info("Vehicle fully charged: vehicleId={}", vehicle.id());
                    }
                }

                Thread.sleep(10_000);

            } catch (Exception e) {
                LOG.error("Vehicle charging worker failed", e);

                try {
                    Thread.sleep(10_000);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }
}