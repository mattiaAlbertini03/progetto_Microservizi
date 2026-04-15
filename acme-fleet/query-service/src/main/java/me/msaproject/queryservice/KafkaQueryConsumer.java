package me.msaproject.queryservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class KafkaQueryConsumer {

    private static final Logger LOG = Logger.getLogger(KafkaQueryConsumer.class.getName());
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final long RETRY_DELAY_MS = 5000;

    @Inject
    VehicleStore store;

    @Inject
    @ConfigProperty(name = "kafka.bootstrap.servers", defaultValue = "my-cluster-kafka-bootstrap.kafka:9092")
    String bootstrapServers;

    @Inject
    @ConfigProperty(name = "kafka.topic.vehicle-tracking", defaultValue = "vehicle-tracking")
    String trackingTopic;

    @Inject
    @ConfigProperty(name = "kafka.topic.vehicle-battery", defaultValue = "vehicle-battery")
    String batteryTopic;

    @Inject
    @ConfigProperty(name = "kafka.consumer.group.id", defaultValue = "query-service")
    String groupId;

    @Inject
    @ConfigProperty(name = "kafka.consumer.auto.offset.reset", defaultValue = "earliest")
    String offsetReset;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean running = new AtomicBoolean(true);

    private volatile KafkaConsumer<String, String> consumer;

    @PostConstruct
    void init() {
        executor.submit(this::consumeLoop);
        LOG.info(() -> "Kafka consumer starting. bootstrap.servers=" + bootstrapServers
                + ", topics=[" + trackingTopic + ", " + batteryTopic + "]"
                + ", group.id=" + groupId);
    }

    private void consumeLoop() {
        Properties p = new Properties();
        p.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        p.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        p.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        p.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        p.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, offsetReset);
        p.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        while (running.get() && !Thread.currentThread().isInterrupted()) {
            try (KafkaConsumer<String, String> c = new KafkaConsumer<>(p)) {
                this.consumer = c;
                c.subscribe(List.of(trackingTopic, batteryTopic));

                LOG.info(() -> "Kafka consumer connected and subscribed to topics ["
                        + trackingTopic + ", " + batteryTopic + "]");

                while (running.get() && !Thread.currentThread().isInterrupted()) {
                    ConsumerRecords<String, String> records = c.poll(Duration.ofMillis(500));

                    for (ConsumerRecord<String, String> r : records) {
                        handleRecord(r);
                    }

                    if (!records.isEmpty()) {
                        c.commitSync();
                    }
                }

            } catch (WakeupException e) {
                if (running.get()) {
                    LOG.warning("Kafka consumer wakeup received unexpectedly, retrying...");
                } else {
                    LOG.info("Kafka consumer stopped.");
                }
            } catch (Exception e) {
                if (!running.get() || Thread.currentThread().isInterrupted()) {
                    LOG.info("Kafka consumer stopping.");
                    break;
                }

                LOG.log(Level.SEVERE,
                        "Kafka consumer crashed. Retrying in " + RETRY_DELAY_MS + " ms",
                        e);

                sleepBeforeRetry();
            } finally {
                consumer = null;
            }
        }

        LOG.info("Kafka consumer loop terminated.");
    }

    private void sleepBeforeRetry() {
        try {
            Thread.sleep(RETRY_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void handleRecord(ConsumerRecord<String, String> r) throws Exception {
        JsonNode node = MAPPER.readTree(r.value());

        String vehicleId = r.key();
        if (vehicleId == null || vehicleId.isBlank()) {
            JsonNode vehicleIdNode = node.get("vehicleId");
            vehicleId = vehicleIdNode != null ? vehicleIdNode.asText() : null;
        }

        if (vehicleId == null || vehicleId.isBlank()) {
            throw new IllegalArgumentException("Missing vehicleId in record from topic " + r.topic());
        }

        long timestamp = node.get("timestampEpochMs").asLong();

        if (trackingTopic.equals(r.topic())) {
            double lat = node.get("lat").asDouble();
            double lon = node.get("lon").asDouble();
            store.upsertTracking(vehicleId, lat, lon, timestamp);
        } else if (batteryTopic.equals(r.topic())) {
            double batteryPct = node.get("batteryPct").asDouble();
            store.upsertBattery(vehicleId, batteryPct, timestamp);
        } else {
            LOG.warning(() -> "Unexpected topic: " + r.topic());
        }
    }

    @PreDestroy
    void destroy() {
        running.set(false);

        KafkaConsumer<String, String> c = consumer;
        if (c != null) {
            c.wakeup();
        }

        executor.shutdownNow();
    }
}