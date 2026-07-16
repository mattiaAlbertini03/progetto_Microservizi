package me.msaproject.logisticsservice.workers;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import me.msaproject.logisticsservice.logistics.LogisticsService;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class WorkerStarter {

    private static final Logger LOG =
            LoggerFactory.getLogger(WorkerStarter.class);

    @Inject
    LogisticsService logisticsService;

    @Inject
    @ConfigProperty(name = "logistics.workers.enabled", defaultValue = "true")
    boolean workersEnabled;

    public void onStart(@Observes @Initialized(ApplicationScoped.class) Object event) {
        if (!workersEnabled) {
            LOG.info("Logistics workers are disabled");
            return;
        }

        LOG.info("Starting logistics workers...");

        Thread chargingMonitorWorker = new Thread(
                new ChargingMonitorWorker(logisticsService),
                "charging-monitor-worker"
        );

        Thread vehicleChargingWorker = new Thread(
                new VehicleChargingWorker(logisticsService),
                "vehicle-charging-worker"
        );

        chargingMonitorWorker.setDaemon(true);
        vehicleChargingWorker.setDaemon(true);

        chargingMonitorWorker.start();
        vehicleChargingWorker.start();

        LOG.info("All logistics workers started");
    }
}
