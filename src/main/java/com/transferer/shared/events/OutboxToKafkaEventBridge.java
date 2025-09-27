package com.transferer.shared.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.transferer.shared.domain.events.DomainEvent;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;
import java.time.Instant;

@Component
@ConditionalOnProperty(name = "transferer.eventbus.bridge.enabled", havingValue = "true")
public class OutboxToKafkaEventBridge {
    
    private static final Logger logger = LoggerFactory.getLogger(OutboxToKafkaEventBridge.class);
    
    private final EventBus outboxEventBus;
    private final ReactiveKafkaProducerTemplate<String, String> kafkaProducer;
    private final ObjectMapper objectMapper;
    private final String kafkaTopicName;
    
    public OutboxToKafkaEventBridge(
            EventBus outboxEventBus,
            ReactiveKafkaProducerTemplate<String, String> kafkaProducer,
            ObjectMapper objectMapper,
            @Value("${transferer.kafka.topic.events:domain-events}") String kafkaTopicName) {
        this.outboxEventBus = outboxEventBus;
        this.kafkaProducer = kafkaProducer;
        this.objectMapper = objectMapper;
        this.kafkaTopicName = kafkaTopicName;
    }
    
    @PostConstruct
    public void startBridge() {
        try {
            logger.info("Starting OutboxToKafka event bridge, publishing to topic: {}", kafkaTopicName);
            
            outboxEventBus.eventStream()
                    .flatMap(this::publishToKafka)
                    .doOnError(error -> logger.error("Error in OutboxToKafka bridge: {}", error.getMessage(), error))
                    .retry()
                    .subscribe();
                    
            logger.info("OutboxToKafka event bridge started successfully");
        } catch (Exception e) {
            logger.error("Failed to start OutboxToKafka event bridge: {}", e.getMessage(), e);
            // Don't throw - allow the application to start even if bridge fails
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
                    record.headers().add("source", "outbox-bridge".getBytes());
                    
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