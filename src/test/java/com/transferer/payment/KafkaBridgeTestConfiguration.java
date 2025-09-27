package com.transferer.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transferer.shared.events.EventBus;
import com.transferer.shared.events.KafkaEventBus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.transferer.shared.domain.events.DomainEvent;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import java.time.Instant;
import com.transferer.shared.outbox.OutboxEventBus;
import com.transferer.shared.outbox.OutboxEventRepository;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;

import jakarta.annotation.PreDestroy;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaBridgeTestConfiguration {
    
    private static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));
    private static final String TEST_TOPIC = "test-domain-events";
    
    static {
        if (!kafka.isRunning()) {
            kafka.start();
        }
    }
    
    private String getKafkaBootstrapServers() {
        // Ensure container is started before accessing ports
        if (!kafka.isRunning()) {
            kafka.start();
        }
        return kafka.getBootstrapServers();
    }
    
    @PreDestroy
    public void cleanup() {
        kafka.stop();
    }
    
    @Bean
    public OutboxEventBus outboxEventBus(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        return new OutboxEventBus(outboxEventRepository, objectMapper);
    }
    
    @Bean
    public ReactiveKafkaProducerTemplate<String, String> bridgeKafkaProducerTemplate() {
        Map<String, Object> producerProps = new HashMap<>();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, getKafkaBootstrapServers());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.ACKS_CONFIG, "all");
        producerProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        producerProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        
        SenderOptions<String, String> senderOptions = SenderOptions.create(producerProps);
        KafkaSender<String, String> kafkaSender = KafkaSender.create(senderOptions);
        
        return new ReactiveKafkaProducerTemplate<>(kafkaSender);
    }
    
    @Bean("kafkaEventBusProducerTemplate")
    public ReactiveKafkaProducerTemplate<String, String> kafkaEventBusProducerTemplate() {
        Map<String, Object> producerProps = new HashMap<>();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, getKafkaBootstrapServers());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.ACKS_CONFIG, "all");
        producerProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        producerProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        
        SenderOptions<String, String> senderOptions = SenderOptions.create(producerProps);
        KafkaSender<String, String> kafkaSender = KafkaSender.create(senderOptions);
        
        return new ReactiveKafkaProducerTemplate<>(kafkaSender);
    }
    
    @Bean
    public KafkaReceiver<String, String> kafkaReceiver() {
        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, getKafkaBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-payment-saga");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        
        ReceiverOptions<String, String> receiverOptions = ReceiverOptions.<String, String>create(consumerProps)
                .subscription(Collections.singleton(TEST_TOPIC));
        
        return KafkaReceiver.create(receiverOptions);
    }

    @Bean
    public TestOutboxToKafkaEventBridge outboxToKafkaEventBridge(
            OutboxEventBus outboxEventBus,
            ReactiveKafkaProducerTemplate<String, String> bridgeKafkaProducerTemplate,
            ObjectMapper objectMapper) {
        return new TestOutboxToKafkaEventBridge(outboxEventBus, bridgeKafkaProducerTemplate, objectMapper, TEST_TOPIC);
    }
    
    @Bean
    public EventBus kafkaEventBus(ObjectMapper objectMapper) {
        return new KafkaEventBus(kafkaEventBusProducerTemplate(), kafkaReceiver(), objectMapper, TEST_TOPIC);
    }

    // Test-specific implementation of OutboxToKafkaEventBridge without conditional annotation
    public static class TestOutboxToKafkaEventBridge {

        private static final Logger logger = LoggerFactory.getLogger(TestOutboxToKafkaEventBridge.class);

        private final EventBus outboxEventBus;
        private final ReactiveKafkaProducerTemplate<String, String> kafkaProducer;
        private final ObjectMapper objectMapper;
        private final String kafkaTopicName;
        private boolean bridgeStarted = false;

        public TestOutboxToKafkaEventBridge(
                EventBus outboxEventBus,
                ReactiveKafkaProducerTemplate<String, String> kafkaProducer,
                ObjectMapper objectMapper,
                String kafkaTopicName) {
            this.outboxEventBus = outboxEventBus;
            this.kafkaProducer = kafkaProducer;
            this.objectMapper = objectMapper;
            this.kafkaTopicName = kafkaTopicName;
        }

        public void startBridge() {
            if (bridgeStarted) {
                logger.debug("Bridge already started, skipping");
                return;
            }

            try {
                logger.info("Starting TestOutboxToKafka event bridge, publishing to topic: {}", kafkaTopicName);

                outboxEventBus.eventStream()
                        .flatMap(this::publishToKafka)
                        .doOnError(error -> logger.error("Error in TestOutboxToKafka bridge: {}", error.getMessage(), error))
                        .retry()
                        .subscribe();

                bridgeStarted = true;
                logger.info("TestOutboxToKafka event bridge started successfully");
            } catch (Exception e) {
                logger.error("Failed to start TestOutboxToKafka event bridge: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to start test bridge", e);
            }
        }

        private Mono<Void> publishToKafka(DomainEvent<?> event) {
            return serializeEventForKafka(event)
                    .flatMap(eventJson -> {
                        ProducerRecord<String, String> record = new ProducerRecord<>(
                                kafkaTopicName,
                                event.getAggregateId(),
                                eventJson
                        );

                        // Add headers for easier consumption
                        record.headers().add("eventType", event.getEventType().name().getBytes());
                        record.headers().add("eventId", event.getEventId().getBytes());
                        record.headers().add("aggregateId", event.getAggregateId().getBytes());
                        record.headers().add("occurredAt", event.getOccurredAt().toString().getBytes());
                        record.headers().add("source", "test-outbox-bridge".getBytes());

                        return kafkaProducer.send(record);
                    })
                    .doOnNext(result -> logger.debug("Bridged event {} from outbox to Kafka topic {}",
                            event.getEventId(), kafkaTopicName))
                    .doOnError(error -> logger.error("Failed to bridge event {} to Kafka: {}",
                            event.getEventId(), error.getMessage(), error))
                    .then();
        }

        private Mono<String> serializeEventForKafka(DomainEvent<?> event) {
            return Mono.fromCallable(() -> {
                try {
                    KafkaEventEnvelope envelope = new KafkaEventEnvelope(
                            event.getEventId(),
                            event.getEventType(),
                            event.getAggregateId(),
                            event.getOccurredAt(),
                            objectMapper.writeValueAsString(event.getBody())
                    );
                    return objectMapper.writeValueAsString(envelope);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException("Failed to serialize event for Kafka bridge: " + event.getEventId(), e);
                }
            });
        }

        // Event envelope for Kafka serialization
        private static class KafkaEventEnvelope {
            private final String eventId;
            private final String eventType;
            private final String aggregateId;
            private final Instant occurredAt;
            private final String eventBody;

            public KafkaEventEnvelope(String eventId,
                                    com.transferer.shared.domain.events.DomainEventType eventType,
                                    String aggregateId,
                                    Instant occurredAt,
                                    String eventBody) {
                this.eventId = eventId;
                this.eventType = eventType.name();
                this.aggregateId = aggregateId;
                this.occurredAt = occurredAt;
                this.eventBody = eventBody;
            }

            // Getters for Jackson serialization
            public String getEventId() { return eventId; }
            public String getEventType() { return eventType; }
            public String getAggregateId() { return aggregateId; }
            public Instant getOccurredAt() { return occurredAt; }
            public String getEventBody() { return eventBody; }
        }
    }
}