package me.msaproject.queryservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class QueryConsumer {

    private static final Logger LOG = Logger.getLogger(QueryConsumer.class.getName());
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Inject
    VehicleStore store;

    @Inject
    @ConfigProperty(name = "kafka.bootstrap.servers", defaultValue = "my-cluster-kafka-bootstrap.kafka:9092")
    String bootstrap;

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

    private final ExecutorService exec = Executors.newFixedThreadPool(2);
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean running = new AtomicBoolean(true);

    // teniamo i consumer per poter chiamare wakeup() su shutdown
    private volatile KafkaConsumer<String, String> trackingConsumer;
    private volatile KafkaConsumer<String, String> batteryConsumer;

    // Bootstrap deterministico all'avvio dell'ApplicationScoped context
    void onAppStart(@Observes @Initialized(ApplicationScoped.class) Object init) {
        ensureStarted("cdi-app-init");
    }

    @PostConstruct
    void start() {
        ensureStarted("@PostConstruct");
    }

    protected void ensureStarted(String reason) {
        if (!started.compareAndSet(false, true)) {
            return;
        }

        LOG.warning(() -> "Starting QueryConsumer (" + reason + "). bootstrap=" + bootstrap
                + " trackingTopic=" + trackingTopic + " batteryTopic=" + batteryTopic
                + " groupBase=" + groupId + " offsetReset=" + offsetReset);

        exec.submit(new TopicWorker(
                "tracking",
                trackingTopic,
                groupId + "-tracking",
                this::handleTrackingRecord,
                c -> trackingConsumer = c
        ));

        exec.submit(new TopicWorker(
                "battery",
                batteryTopic,
                groupId + "-battery",
                this::handleBatteryRecord,
                c -> batteryConsumer = c
        ));
    }

    private void handleTrackingRecord(ConsumerRecord<String, String> r) throws Exception {
        String vehicleId = r.key();
        String json = r.value();

        var node = MAPPER.readTree(json);
        double lat = node.get("lat").asDouble();
        double lon = node.get("lon").asDouble();
        long ts = node.get("timestampEpochMs").asLong();

        if (vehicleId == null || vehicleId.isBlank()) {
            vehicleId = node.get("vehicleId").asText();
        }

        store.upsertTracking(vehicleId, lat, lon, ts);
    }

    private void handleBatteryRecord(ConsumerRecord<String, String> r) throws Exception {
        String vehicleId = r.key();
        String json = r.value();

        var node = MAPPER.readTree(json);
        // nel tuo topic hai visto anche 82.0 -> asInt() va bene (tronca), ma lo rendiamo esplicito
        int batteryPct = (int) Math.round(node.get("batteryPct").asDouble());
        long ts = node.get("timestampEpochMs").asLong();

        if (vehicleId == null || vehicleId.isBlank()) {
            vehicleId = node.get("vehicleId").asText();
        }

        store.upsertBattery(vehicleId, batteryPct, ts);
    }

    private Properties baseConsumerProps(String gid) {
        Properties p = new Properties();
        p.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        p.put(ConsumerConfig.GROUP_ID_CONFIG, gid);
        p.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringDeserializer.class.getName());
        p.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringDeserializer.class.getName());
        p.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, offsetReset);

        // Strimzi blog style: gestisci tu il commit
        p.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        // tuning “safe defaults” per evitare loop troppo lenti o batch enormi
        p.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "200");
        return p;
    }

    @PreDestroy
    void stop() {
        running.set(false);

        // wakeup: sblocca poll() e consente chiusura pulita
        KafkaConsumer<String, String> tc = trackingConsumer;
        if (tc != null) tc.wakeup();

        KafkaConsumer<String, String> bc = batteryConsumer;
        if (bc != null) bc.wakeup();

        exec.shutdownNow();
    }

    // ===== Worker per singolo topic (1 consumer == 1 thread) =====

    @FunctionalInterface
    private interface RecordHandler {
        void handle(ConsumerRecord<String, String> r) throws Exception;
    }

    @FunctionalInterface
    private interface ConsumerSink {
        void accept(KafkaConsumer<String, String> c);
    }

    private final class TopicWorker implements Runnable, ConsumerRebalanceListener {
        private final String name;
        private final String topic;
        private final String gid;
        private final RecordHandler handler;
        private final ConsumerSink sink;

        private final Map<TopicPartition, OffsetAndMetadata> pendingCommits = new ConcurrentHashMap<>();
        private KafkaConsumer<String, String> consumer;

        private TopicWorker(String name, String topic, String gid, RecordHandler handler, ConsumerSink sink) {
            this.name = name;
            this.topic = topic;
            this.gid = gid;
            this.handler = handler;
            this.sink = sink;
        }

        @Override
        public void run() {
            Properties props = baseConsumerProps(gid);

            try (KafkaConsumer<String, String> c = new KafkaConsumer<>(props)) {
                this.consumer = c;
                sink.accept(c);

                c.subscribe(List.of(topic), this);
                LOG.warning(() -> "[" + name + "] subscribed. topic=" + topic + " groupId=" + gid);

                while (running.get() && !Thread.currentThread().isInterrupted()) {
                    ConsumerRecords<String, String> records = c.poll(Duration.ofMillis(500));

                    if (!records.isEmpty()) {
                        for (ConsumerRecord<String, String> r : records) {
                            handler.handle(r);

                            // memorizza offset “next” da committare
                            pendingCommits.put(
                                    new TopicPartition(r.topic(), r.partition()),
                                    new OffsetAndMetadata(r.offset() + 1)
                            );
                        }

                        // commit async “normale”
                        Map<TopicPartition, OffsetAndMetadata> snapshot = new HashMap<>(pendingCommits);
                        c.commitAsync(snapshot, (offsets, ex) -> {
                            if (ex != null) {
                                LOG.log(Level.SEVERE, "[" + name + "] commitAsync failed", ex);
                            }
                        });
                    }
                }

                // uscita: commit sync finale
                if (!pendingCommits.isEmpty()) {
                    c.commitSync(new HashMap<>(pendingCommits));
                }

            } catch (WakeupException we) {
                // shutdown “normale”: commit sync finale se possibile
                try {
                    if (consumer != null && !pendingCommits.isEmpty()) {
                        consumer.commitSync(new HashMap<>(pendingCommits));
                    }
                } catch (Exception e) {
                    LOG.log(Level.SEVERE, "[" + name + "] commitSync on wakeup failed", e);
                }
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "[" + name + "] consumer crashed", e);
            } finally {
                LOG.warning(() -> "[" + name + "] worker stopped.");
            }
        }

        @Override
        public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
            // best practice: commit prima di perdere partizioni
            try {
                if (consumer != null && !pendingCommits.isEmpty()) {
                    consumer.commitSync(new HashMap<>(pendingCommits));
                }
                LOG.warning(() -> "[" + name + "] partitions revoked: " + partitions);
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "[" + name + "] commitSync on revoke failed", e);
            }
        }

        @Override
        public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
            LOG.warning(() -> "[" + name + "] partitions assigned: " + partitions);
        }
    }
}
