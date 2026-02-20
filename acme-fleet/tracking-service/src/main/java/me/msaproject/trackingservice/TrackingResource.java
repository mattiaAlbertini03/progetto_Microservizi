package me.msaproject.trackingservice;

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
@Path("/tracking")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class TrackingResource {

    private static final Logger LOG = Logger.getLogger(TrackingResource.class.getName());

    @Inject
    KafkaTrackingProducer producer;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    @ConfigProperty(name = "tracking.api.key")
    Optional<String> apiKey;

    @POST
    public Response publish(@HeaderParam("X-API-Key") String providedKey,
                            VehicleTrackingEvent event) {

        // --- Auth opzionale ---
        String configuredKey = apiKey.orElse("").trim();
            if (!configuredKey.isBlank()) {
                if (providedKey == null || !configuredKey.equals(providedKey)) {
                    return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("error", "unauthorized"))
                    .build();
                }
        }


        // --- Validazioni minime ma serie ---
        if (event == null) {
            throw new BadRequestException("Body is required");
        }
        if (event.vehicleId() == null || event.vehicleId().isBlank()) {
            throw new BadRequestException("vehicleId is required");
        }
        if (event.lat() < -90.0 || event.lat() > 90.0) {
            throw new BadRequestException("lat must be in [-90, 90]");
        }
        if (event.lon() < -180.0 || event.lon() > 180.0) {
            throw new BadRequestException("lon must be in [-180, 180]");
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
            VehicleTrackingEvent normalized = new VehicleTrackingEvent(
                    event.vehicleId(),
                    event.lat(),
                    event.lon(),
                    ts
            );

            String json = objectMapper.writeValueAsString(normalized);

            // key = vehicleId
            producer.publish(normalized.vehicleId(), json);

            LOG.info(() -> "Tracking accepted vehicleId=" + normalized.vehicleId()
                    + " lat=" + normalized.lat()
                    + " lon=" + normalized.lon()
                    + " ts=" + normalized.timestampEpochMs());

            // 202: accettato e messo in coda al producer (“consegnato” NON garantito)
            return Response.accepted(Map.of("status", "accepted")).build();

        } catch (Exception e) {
            // se Kafka è giù o serialization fallisce ecc.
            LOG.severe("Failed to publish tracking: " + e.getMessage());
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(Map.of("error", "kafka_publish_failed"))
                    .build();
        }
    }
}
