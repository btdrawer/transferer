package com.transferer.shared.events;

import com.transferer.shared.domain.events.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@ConditionalOnProperty(name = "outbox.enabled", havingValue = "false", matchIfMissing = false)
public class InMemoryEventPublisher implements EventPublisher {
    
    private static final Logger logger = LoggerFactory.getLogger(InMemoryEventPublisher.class);
    
    private final ApplicationEventPublisher applicationEventPublisher;
    
    public InMemoryEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }
    
    @Override
    public Mono<Void> publish(DomainEvent event) {
        return Mono.fromRunnable(() -> {
            logger.info("Publishing event: {} with ID: {}", event.getEventType(), event.getEventId());
            applicationEventPublisher.publishEvent(event);
        });
    }
}