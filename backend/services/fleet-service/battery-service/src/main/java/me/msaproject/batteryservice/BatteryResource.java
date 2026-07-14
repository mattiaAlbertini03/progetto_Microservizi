package me.msaproject.batteryservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

@ApplicationScoped
@Path("/battery")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class BatteryResource {

    private static final Logger LOG = Logger.getLogger(BatteryResource.class.getName());

    @Inject
    KafkaBatteryProducer producer;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    @ConfigProperty(name = "battery.api.key")
    Optional<String> apiKey;

    @POST
    public Response publish(@HeaderParam("X-API-Key") String providedKey,
            VehicleBatteryEvent event) {

        // --- Auth opzionale ---
        String configuredKey = apiKey.orElse("").trim();
        if (!configuredKey.isBlank()) {
            if (providedKey == null || !configuredKey.equals(providedKey)) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(Map.of("error", "unauthorized"))
                        .build();
            }
        }

        // --- Validazioni minime ---
        if (event == null) {
            throw new BadRequestException("Body is required");
        }
        if (event.vehicleId() == null || event.vehicleId().isBlank()) {
            throw new BadRequestException("vehicleId is required");
        }
        if (event.batteryPct() < 0 || event.batteryPct() > 100.0) {
            throw new BadRequestException("batteryPct must be in [0, 100]");
        }

        long ts = (event.timestampEpochMs() != null) ? event.timestampEpochMs() : System.currentTimeMillis();

        // timestamp non negativo + non troppo nel futuro (tolleranza 5 min)
        long now = System.currentTimeMillis();
        if (ts < 0) {
            throw new BadRequestException("timestampEpochMs must be >= 0");
        }
        if (ts > now + 5 * 60_000L) {
            throw new BadRequestException("timestampEpochMs is too far in the future");
        }

        try {
            // normalizziamo l’evento (timestamp sempre presente)
            VehicleBatteryEvent normalized = new VehicleBatteryEvent(
                    event.vehicleId(),
                    event.batteryPct(),
                    ts);

            String json = objectMapper.writeValueAsString(normalized);

            // key = vehicleId
            producer.publish(normalized.vehicleId(), json);

            LOG.info(() -> "Battery accepted vehicleId=" + normalized.vehicleId()
                    + " batteryPct=" + normalized.batteryPct()
                    + " ts=" + normalized.timestampEpochMs());

            // 202: accettato e messo in coda al producer (non “consegnato” garantito)
            return Response.accepted(Map.of("status", "accepted")).build();

        } catch (Exception e) {
            LOG.log(java.util.logging.Level.SEVERE, "Failed to publish battery", e);
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(Map.of("error", "kafka_publish_failed"))
                    .build();
        }
    }
}