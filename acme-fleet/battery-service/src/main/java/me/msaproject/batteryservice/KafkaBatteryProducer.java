package me.msaproject.batteryservice;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.annotation.Metric;

import java.time.Duration;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class KafkaBatteryProducer {

    private static final Logger LOG = Logger.getLogger(KafkaBatteryProducer.class.getName());

    @Inject
    @ConfigProperty(name = "kafka.bootstrap.servers", defaultValue = "my-cluster-kafka-bootstrap.kafka:9092")
    String bootstrapServers;

    @Inject
    @ConfigProperty(name = "kafka.topic.vehicle-battery", defaultValue = "vehicle-battery")
    String topic;

    @Inject
    @ConfigProperty(name = "kafka.producer.acks", defaultValue = "all")
    String acks;

    @Inject
    @ConfigProperty(name = "kafka.producer.enable.idempotence", defaultValue = "true")
    boolean enableIdempotence;

    @Inject
    @ConfigProperty(name = "kafka.producer.linger.ms", defaultValue = "5")
    int lingerMs;

    // ---- Correzioni per affidabilità (senza sincronia) ----

    @Inject
    @ConfigProperty(name = "kafka.producer.retries", defaultValue = "2147483647")
    int retries;

    @Inject
    @ConfigProperty(name = "kafka.producer.delivery.timeout.ms", defaultValue = "120000")
    int deliveryTimeoutMs;

    @Inject
    @ConfigProperty(name = "kafka.producer.request.timeout.ms", defaultValue = "30000")
    int requestTimeoutMs;

    @Inject
    @ConfigProperty(name = "kafka.producer.max.block.ms", defaultValue = "3000")
    int maxBlockMs;

    @Inject
    @ConfigProperty(name = "kafka.producer.compression.type", defaultValue = "snappy")
    String compressionType;

    @Inject
    @ConfigProperty(name = "kafka.producer.client.id", defaultValue = "battery-service-producer")
    String clientId;

    @Inject
    @Metric(name = "battery_events_published_total", absolute = true)
    Counter published;

    @Inject
    @Metric(name = "battery_events_failed_total", absolute = true)
    Counter failed;

    private KafkaProducer<String, String> producer;

    @PostConstruct
    void init() {
        Properties p = new Properties();
        p.put("bootstrap.servers", bootstrapServers);
        p.put("key.serializer", StringSerializer.class.getName());
        p.put("value.serializer", StringSerializer.class.getName());

        // Affidabilità
        p.put("acks", acks);
        p.put("enable.idempotence", Boolean.toString(enableIdempotence));
        p.put("retries", Integer.toString(retries));
        p.put("delivery.timeout.ms", Integer.toString(deliveryTimeoutMs));
        p.put("request.timeout.ms", Integer.toString(requestTimeoutMs));
        p.put("max.block.ms", Integer.toString(maxBlockMs));

        // Throughput/efficienza
        p.put("linger.ms", Integer.toString(lingerMs));
        p.put("compression.type", compressionType);

        // Identificazione client
        p.put("client.id", clientId);

        producer = new KafkaProducer<>(p);
        LOG.info(() -> "Kafka producer initialized. bootstrap.servers=" + bootstrapServers
                + ", topic=" + topic
                + ", client.id=" + clientId);
    }

    public void publish(String vehicleId, String jsonPayload) {
        try {
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, vehicleId, jsonPayload);

            producer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    failed.inc();
                    LOG.log(Level.SEVERE, "Kafka publish failed for vehicleId=" + vehicleId, exception);
                } else {
                    published.inc();
                    LOG.fine(() -> "Kafka published vehicleId=" + vehicleId
                            + " topic=" + metadata.topic()
                            + " partition=" + metadata.partition()
                            + " offset=" + metadata.offset());
                }
            });
        } catch (Exception e) {
            failed.inc();
            LOG.log(Level.SEVERE, "Kafka publish threw exception for vehicleId=" + vehicleId, e);
            throw e;
        }
    }

    @PreDestroy
    void destroy() {
        if (producer != null) {
            try {
                // riduce perdita in shutdown "ordinato"
                producer.flush();
                producer.close(Duration.ofSeconds(5));
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Error closing Kafka producer", e);
            }
        }
    }
}
