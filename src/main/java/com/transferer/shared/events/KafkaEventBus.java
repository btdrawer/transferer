package com.transferer.shared.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.transferer.shared.domain.events.DomainEvent;
import com.transferer.shared.domain.events.DomainEventType;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverRecord;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

@Component
@ConditionalOnProperty(name = "transferer.eventbus.type", havingValue = "kafka")
public class KafkaEventBus implements EventBus {
    
    private static final Logger logger = LoggerFactory.getLogger(KafkaEventBus.class);
    
    private final ReactiveKafkaProducerTemplate<String, String> kafkaProducer;
    private final KafkaReceiver<String, String> kafkaReceiver;
    private final ObjectMapper objectMapper;
    private final String topicName;
    private final Sinks.Many<DomainEvent<?>> eventSink;
    private final ConcurrentHashMap<Class<? extends DomainEvent<?>>, CopyOnWriteArrayList<Consumer<DomainEvent<?>>>> subscribers;
    
    public KafkaEventBus(
            ReactiveKafkaProducerTemplate<String, String> kafkaProducer,
            KafkaReceiver<String, String> kafkaReceiver,
            ObjectMapper objectMapper,
            @Value("${transferer.kafka.topic.events:domain-events}") String topicName) {
        this.kafkaProducer = kafkaProducer;
        this.kafkaReceiver = kafkaReceiver;
        this.objectMapper = objectMapper;
        this.topicName = topicName;
        this.eventSink = Sinks.many().multicast().onBackpressureBuffer();
        this.subscribers = new ConcurrentHashMap<>();
        
        // Start consuming messages from Kafka
        startKafkaConsumer();
    }
    
    @Override
    public Mono<Void> publish(DomainEvent<?> event) {
        return serializeEventForKafka(event)
                .flatMap(eventJson -> {
                    ProducerRecord<String, String> record = new ProducerRecord<>(
                            topicName,
                            event.getAggregateId(),
                            eventJson
                    );
                    record.headers().add("eventType", event.getEventType().name().getBytes());
                    record.headers().add("eventId", event.getEventId().getBytes());
                    record.headers().add("aggregateId", event.getAggregateId().getBytes());
                    
                    return kafkaProducer.send(record);
                })
                .doOnNext(result -> logger.debug("Published event {} to Kafka topic {}", 
                        event.getEventId(), topicName))
                .doOnError(error -> logger.error("Failed to publish event {} to Kafka: {}", 
                        event.getEventId(), error.getMessage(), error))
                .then();
    }

    @Override
    public <T extends DomainEvent<?>> void subscribe(Class<T> eventClass, Consumer<T> handler) {
        subscribers.computeIfAbsent(
                eventClass,
                k -> new CopyOnWriteArrayList<>()
        ).add(event -> {
            if (eventClass.isInstance(event)) {
                handler.accept(eventClass.cast(event));
            }
        });
        logger.debug("Subscribed handler to event class: {}", eventClass.getSimpleName());
    }
    
    @Override
    public Flux<DomainEvent<?>> eventStream() {
        return eventSink.asFlux();
    }
    
    @Override
    public Flux<DomainEvent<?>> eventStream(DomainEventType eventType) {
        return eventSink.asFlux()
                .filter(event -> event.getEventType().equals(eventType));
    }
    
    private void startKafkaConsumer() {
        kafkaReceiver.receive()
                .flatMap(this::processKafkaRecord)
                .doOnNext(event -> {
                    // Emit to reactive stream
                    eventSink.tryEmitNext(event);
                    // Notify subscribers
                    notifySubscribers(event);
                })
                .doOnError(error -> logger.error("Error processing Kafka message: {}", error.getMessage(), error))
                .retry()
                .subscribe();
    }
    
    private Mono<DomainEvent<?>> processKafkaRecord(ReceiverRecord<String, String> record) {
        return Mono.<DomainEvent<?>>fromCallable(() -> {
            String eventTypeHeader = new String(record.headers().lastHeader("eventType").value());
            String eventIdHeader = new String(record.headers().lastHeader("eventId").value());
            String aggregateIdHeader = new String(record.headers().lastHeader("aggregateId").value());
            
            DomainEventType eventType = DomainEventType.valueOf(eventTypeHeader);
            String eventJson = record.value();
            
            // This is a simplified deserialization - in practice, you'd need a registry
            // of event types to properly deserialize to the correct DomainEvent subclass
            return deserializeKafkaEvent(eventType, eventIdHeader, aggregateIdHeader, eventJson);
        })
        .doOnNext(event -> record.receiverOffset().acknowledge())
        .doOnError(error -> {
            logger.error("Failed to process Kafka record: {}", error.getMessage(), error);
            record.receiverOffset().acknowledge(); // Acknowledge to avoid reprocessing
        });
    }
    
    private Mono<String> serializeEventForKafka(DomainEvent<?> event) {
        return Mono.fromCallable(() -> {
            try {
                // Create a simplified JSON representation for Kafka
                KafkaEventEnvelope envelope = new KafkaEventEnvelope(
                        event.getEventId(),
                        event.getEventType(),
                        event.getAggregateId(),
                        event.getOccurredAt(),
                        objectMapper.writeValueAsString(event.getBody())
                );
                return objectMapper.writeValueAsString(envelope);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize event for Kafka: " + event.getEventId(), e);
            }
        });
    }
    
    private DomainEvent<?> deserializeKafkaEvent(DomainEventType eventType, String eventId, String aggregateId, String eventJson) {
        // This is a placeholder implementation
        // In a real system, you'd need a proper event registry to deserialize to concrete types
        throw new UnsupportedOperationException("Event deserialization not yet implemented - requires event type registry");
    }
    
    private void notifySubscribers(DomainEvent<?> event) {
        CopyOnWriteArrayList<Consumer<DomainEvent<?>>> handlers = subscribers.get(event.getClass());
        if (handlers != null) {
            handlers.forEach(handler -> {
                try {
                    handler.accept(event);
                } catch (Exception e) {
                    logger.error("Error in event handler for class {}: {}", 
                            event.getClass().getSimpleName(), e.getMessage(), e);
                }
            });
        }
    }
    
    // Inner class for Kafka event envelope
    private static class KafkaEventEnvelope {
        private final String eventId;
        private final DomainEventType eventType;
        private final String aggregateId;
        private final java.time.Instant occurredAt;
        private final String eventBody;
        
        public KafkaEventEnvelope(String eventId, DomainEventType eventType, String aggregateId, 
                                java.time.Instant occurredAt, String eventBody) {
            this.eventId = eventId;
            this.eventType = eventType;
            this.aggregateId = aggregateId;
            this.occurredAt = occurredAt;
            this.eventBody = eventBody;
        }
        
        // Getters for Jackson serialization
        public String getEventId() { return eventId; }
        public DomainEventType getEventType() { return eventType; }
        public String getAggregateId() { return aggregateId; }
        public java.time.Instant getOccurredAt() { return occurredAt; }
        public String getEventBody() { return eventBody; }
    }
}