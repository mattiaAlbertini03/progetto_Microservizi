package me.msaproject.queryservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
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

    private static final Logger LOG =
            Logger.getLogger(KafkaQueryConsumer.class.getName());

    private static final ObjectMapper MAPPER =
            new ObjectMapper();

    private static final long RETRY_DELAY_MS = 5000;

    @Inject
    VehicleStore store;

    @Inject
    @ConfigProperty(
            name = "kafka.consumer.enabled",
            defaultValue = "true"
    )
    boolean enabled;

    @Inject
    @ConfigProperty(
            name = "kafka.bootstrap.servers",
            defaultValue = "my-cluster-kafka-bootstrap.kafka:9092"
    )
    String bootstrapServers;

    @Inject
    @ConfigProperty(
            name = "kafka.topic.vehicle-tracking",
            defaultValue = "vehicle-tracking"
    )
    String trackingTopic;

    @Inject
    @ConfigProperty(
            name = "kafka.topic.vehicle-battery",
            defaultValue = "vehicle-battery"
    )
    String batteryTopic;

    @Inject
    @ConfigProperty(
            name = "kafka.consumer.group.id",
            defaultValue = "query-service"
    )
    String groupId;

    @Inject
    @ConfigProperty(
            name = "kafka.consumer.auto.offset.reset",
            defaultValue = "earliest"
    )
    String offsetReset;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor(runnable -> {
                Thread thread =
                        new Thread(runnable, "query-kafka-consumer");

                thread.setDaemon(true);

                return thread;
            });

    private final AtomicBoolean running =
            new AtomicBoolean(false);

    private volatile KafkaConsumer<String, String> consumer;

    /**
     * Avvia il consumer non appena viene inizializzato
     * l'ApplicationScoped CDI.
     */
    void onApplicationStarted(
            @Observes
            @Initialized(ApplicationScoped.class)
            Object ignored
    ) {
        if (!enabled) {
            LOG.info("Kafka consumer is disabled.");
            return;
        }

        startConsumer();
    }

    private void startConsumer() {
        if (!running.compareAndSet(false, true)) {
            LOG.warning(
                    "Kafka consumer already started; duplicate startup ignored."
            );
            return;
        }

        LOG.info(() ->
                "Kafka consumer starting."
                        + " bootstrap.servers=" + bootstrapServers
                        + ", topics=[" + trackingTopic
                        + ", " + batteryTopic + "]"
                        + ", group.id=" + groupId
                        + ", auto.offset.reset=" + offsetReset
        );

        executor.submit(this::consumeLoop);
    }

    private void consumeLoop() {
        Properties properties = new Properties();

        properties.put(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                bootstrapServers
        );

        properties.put(
                ConsumerConfig.GROUP_ID_CONFIG,
                groupId
        );

        properties.put(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class.getName()
        );

        properties.put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class.getName()
        );

        properties.put(
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                offsetReset
        );

        properties.put(
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,
                "false"
        );

        while (
                running.get()
                        && !Thread.currentThread().isInterrupted()
        ) {
            try (
                    KafkaConsumer<String, String> kafkaConsumer =
                            new KafkaConsumer<>(properties)
            ) {
                consumer = kafkaConsumer;

                kafkaConsumer.subscribe(
                        List.of(trackingTopic, batteryTopic)
                );

                LOG.info(() ->
                        "Kafka consumer connected and subscribed."
                                + " topics=[" + trackingTopic
                                + ", " + batteryTopic + "]"
                );

                pollContinuously(kafkaConsumer);

            } catch (WakeupException exception) {
                if (running.get()) {
                    LOG.log(
                            Level.WARNING,
                            "Unexpected Kafka consumer wakeup.",
                            exception
                    );
                } else {
                    LOG.info("Kafka consumer stopped by application.");
                }

            } catch (Exception exception) {
                if (
                        !running.get()
                                || Thread.currentThread().isInterrupted()
                ) {
                    LOG.info("Kafka consumer stopping.");
                    break;
                }

                LOG.log(
                        Level.SEVERE,
                        "Kafka consumer crashed. Retrying in "
                                + RETRY_DELAY_MS
                                + " ms.",
                        exception
                );

                sleepBeforeRetry();

            } finally {
                consumer = null;
            }
        }

        LOG.info("Kafka consumer loop terminated.");
    }

    private void pollContinuously(
            KafkaConsumer<String, String> kafkaConsumer
    ) {
        while (
                running.get()
                        && !Thread.currentThread().isInterrupted()
        ) {
            ConsumerRecords<String, String> records =
                    kafkaConsumer.poll(Duration.ofMillis(500));

            for (ConsumerRecord<String, String> record : records) {
                try {
                    handleRecord(record);
                } catch (Exception exception) {
                    /*
                     * Un singolo record malformato non deve far
                     * terminare l'intero consumer.
                     */
                    LOG.log(
                            Level.SEVERE,
                            "Failed to process Kafka record."
                                    + " topic=" + record.topic()
                                    + ", partition=" + record.partition()
                                    + ", offset=" + record.offset()
                                    + ", key=" + record.key()
                                    + ", value=" + record.value(),
                            exception
                    );
                }
            }

            if (!records.isEmpty()) {
                kafkaConsumer.commitSync();

                LOG.fine(() ->
                        "Committed Kafka batch containing "
                                + records.count()
                                + " records."
                );
            }
        }
    }

    private void handleRecord(
            ConsumerRecord<String, String> record
    ) throws Exception {
        JsonNode node = MAPPER.readTree(record.value());

        String vehicleId = extractVehicleId(record, node);
        long timestamp = requiredLong(
                node,
                "timestampEpochMs",
                record.topic()
        );

        if (trackingTopic.equals(record.topic())) {
            double lat = requiredDouble(
                    node,
                    "lat",
                    record.topic()
            );

            double lon = requiredDouble(
                    node,
                    "lon",
                    record.topic()
            );

            store.upsertTracking(
                    vehicleId,
                    lat,
                    lon,
                    timestamp
            );

            LOG.info(() ->
                    "Tracking snapshot updated."
                            + " vehicleId=" + vehicleId
                            + ", lat=" + lat
                            + ", lon=" + lon
                            + ", timestamp=" + timestamp
            );

            return;
        }

        if (batteryTopic.equals(record.topic())) {
            double batteryPct = requiredDouble(
                    node,
                    "batteryPct",
                    record.topic()
            );

            store.upsertBattery(
                    vehicleId,
                    batteryPct,
                    timestamp
            );

            LOG.info(() ->
                    "Battery snapshot updated."
                            + " vehicleId=" + vehicleId
                            + ", batteryPct=" + batteryPct
                            + ", timestamp=" + timestamp
            );

            return;
        }

        LOG.warning(
                "Record received from unexpected topic: "
                        + record.topic()
        );
    }

    private String extractVehicleId(
            ConsumerRecord<String, String> record,
            JsonNode node
    ) {
        String vehicleId = record.key();

        if (vehicleId == null || vehicleId.isBlank()) {
            JsonNode vehicleIdNode = node.get("vehicleId");

            vehicleId = vehicleIdNode != null
                    ? vehicleIdNode.asText()
                    : null;
        }

        if (vehicleId == null || vehicleId.isBlank()) {
            throw new IllegalArgumentException(
                    "Missing vehicleId in record from topic "
                            + record.topic()
            );
        }

        return vehicleId;
    }

    private long requiredLong(
            JsonNode node,
            String fieldName,
            String topic
    ) {
        JsonNode field = node.get(fieldName);

        if (field == null || !field.isNumber()) {
            throw new IllegalArgumentException(
                    "Missing or invalid numeric field '"
                            + fieldName
                            + "' in record from topic "
                            + topic
            );
        }

        return field.asLong();
    }

    private double requiredDouble(
            JsonNode node,
            String fieldName,
            String topic
    ) {
        JsonNode field = node.get(fieldName);

        if (field == null || !field.isNumber()) {
            throw new IllegalArgumentException(
                    "Missing or invalid numeric field '"
                            + fieldName
                            + "' in record from topic "
                            + topic
            );
        }

        return field.asDouble();
    }

    private void sleepBeforeRetry() {
        try {
            Thread.sleep(RETRY_DELAY_MS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    @PreDestroy
    void destroy() {
        if (!running.compareAndSet(true, false)) {
            return;
        }

        KafkaConsumer<String, String> currentConsumer =
                consumer;

        if (currentConsumer != null) {
            currentConsumer.wakeup();
        }

        executor.shutdownNow();

        LOG.info("Kafka consumer shutdown requested.");
    }
}
