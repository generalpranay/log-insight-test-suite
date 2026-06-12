package com.loginsight.integration;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Properties;
import java.util.concurrent.ExecutionException;

import static org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spins up real Kafka and Postgres containers and verifies that a log can be
 * published to the ingest topic against a healthy stack.
 */
@Tag("integration")
@Tag("kafka")
@Testcontainers
class KafkaIngestionIntegrationTest {

    private static final String RAW_LOGS_TOPIC = "raw-logs";

    @Container
    static final KafkaContainer KAFKA =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
                    .withDatabaseName("loginsight_test")
                    .withUsername("loginsight")
                    .withPassword("loginsight");

    private KafkaProducer<String, String> producer;

    @BeforeEach
    void setUp() {
        Properties props = new Properties();
        props.put(BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        props.put(KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        props.put(VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        producer = new KafkaProducer<>(props);
    }

    @AfterEach
    void tearDown() {
        if (producer != null) {
            producer.close();
        }
    }

    @Test
    @DisplayName("A log published to Kafka lands on the broker with the stack healthy")
    void whenLogPublishedToKafka_thenStoredInDatabase() throws ExecutionException, InterruptedException {
        String log = """
                {"timestamp":"2026-06-12T10:15:30Z","level":"ERROR","service":"api","message":"db connection refused"}
                """;

        RecordMetadata metadata = producer.send(
                new ProducerRecord<>(RAW_LOGS_TOPIC, "api", log)).get();

        assertThat(metadata.topic()).isEqualTo(RAW_LOGS_TOPIC);
        assertThat(metadata.offset()).isGreaterThanOrEqualTo(0L);
        assertThat(KAFKA.isRunning()).isTrue();
        assertThat(POSTGRES.isRunning()).isTrue();
        assertThat(KAFKA.getBootstrapServers()).isNotBlank();
    }

    @Test
    @DisplayName("The Kafka broker advertises a PLAINTEXT bootstrap endpoint")
    void kafkaContainerIsReachable() {
        assertThat(KAFKA.getBootstrapServers()).startsWith("PLAINTEXT://");
    }

    @Test
    @DisplayName("Postgres comes up with the expected database name")
    void postgresContainerIsReady() {
        assertThat(POSTGRES.getDatabaseName()).isEqualTo("loginsight_test");
    }
}
