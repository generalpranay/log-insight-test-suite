package com.loginsight.integration;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that payloads which fail validation can be round-tripped through the
 * dead-letter topic, so operators can inspect and replay them.
 */
@Tag("integration")
@Tag("kafka")
@Testcontainers
class DeadLetterQueueTest {

    private static final String DLQ_TOPIC = "raw-logs.DLT";
    private static final String POISON_PAYLOAD = "NOT_VALID_JSON{{{";

    @Container
    static final KafkaContainer KAFKA =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @Test
    @DisplayName("A poison message published to the DLT is consumable from the start of the topic")
    void kafkaDlqTopicIsAccessible() {
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(producerProps())) {
            producer.send(new ProducerRecord<>(DLQ_TOPIC, "poison", POISON_PAYLOAD));
            producer.flush();
        }

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps())) {
            consumer.subscribe(List.of(DLQ_TOPIC));
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));

            assertThat(records.count()).isGreaterThanOrEqualTo(1);
            ConsumerRecord<String, String> first = records.iterator().next();
            assertThat(first.value()).contains("NOT_VALID_JSON");
        }
    }

    private Properties producerProps() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        return props;
    }

    private Properties consumerProps() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "dlq-inspector");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        return props;
    }
}
