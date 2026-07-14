package me.msaproject.logisticsservice.workers;

import me.msaproject.logisticsservice.logistics.LogisticsService;
import me.msaproject.logisticsservice.logistics.Vehicle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class VehicleChargingWorker implements Runnable {

    private static final Logger LOG =
            LoggerFactory.getLogger(VehicleChargingWorker.class);

    private static final int CHARGE_INCREMENT = 2;
    private static final long CHARGE_INTERVAL_MS = 30000;

    private final LogisticsService logisticsService;

    public VehicleChargingWorker(LogisticsService logisticsService) {
        this.logisticsService = logisticsService;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                List<Vehicle> chargingVehicles =
                        logisticsService.getChargingVehicles();

                for (Vehicle vehicle : chargingVehicles) {
                    int nextBatteryPct = Math.min(
                            100,
                            vehicle.batteryPct() + CHARGE_INCREMENT
                    );

                    logisticsService.increaseBattery(
                            vehicle.id(),
                            CHARGE_INCREMENT
                    );

                    LOG.info(
                            "Charging vehicle: vehicleId={}, batteryPct={}",
                            vehicle.id(),
                            nextBatteryPct
                    );

                    if (nextBatteryPct >= 100) {
                        logisticsService.stopCharging(vehicle.id());

                        LOG.info(
                                "Vehicle fully charged: vehicleId={}",
                                vehicle.id()
                        );
                    }
                }

                Thread.sleep(CHARGE_INTERVAL_MS);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;

            } catch (Exception e) {
                LOG.error("Vehicle charging worker failed", e);

                try {
                    Thread.sleep(CHARGE_INTERVAL_MS);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }
}