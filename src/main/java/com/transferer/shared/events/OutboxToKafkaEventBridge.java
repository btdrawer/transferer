package com.transferer.shared.events;

import com.transferer.shared.domain.events.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;

@Component
@ConditionalOnProperty(name = "transferer.eventbus.bridge.enabled", havingValue = "true")
public class OutboxToKafkaEventBridge {
    private static final Logger logger = LoggerFactory.getLogger(OutboxToKafkaEventBridge.class);

    private final EventBus outboxEventBus;
    private final EventPublisher eventPublisher;

    public OutboxToKafkaEventBridge(
            EventBus outboxEventBus,
            EventPublisher eventPublisher) {
        this.outboxEventBus = outboxEventBus;
        this.eventPublisher = eventPublisher;
    }
    
    @PostConstruct
    public void startBridge() {
        try {
            logger.info("Starting OutboxToKafka event bridge");

            outboxEventBus.eventStream()
                    .flatMap(this::publishEvent)
                    .doOnError(error -> logger.error("Error in OutboxToKafka bridge: {}", error.getMessage(), error))
                    .retry()
                    .subscribe();

            logger.info("OutboxToKafka event bridge started successfully");
        } catch (Exception e) {
            logger.error("Failed to start OutboxToKafka event bridge: {}", e.getMessage(), e);
            // Don't throw - allow the application to start even if bridge fails
        }
    }
    
    private Mono<Void> publishEvent(DomainEvent<?> event) {
        return eventPublisher.publish(event)
                .doOnNext(result -> logger.debug("Bridged event {} from outbox to external publisher",
                        event.getEventId()))
                .doOnError(error -> logger.error("Failed to bridge event {} to external publisher: {}",
                        event.getEventId(), error.getMessage(), error));
    }
}