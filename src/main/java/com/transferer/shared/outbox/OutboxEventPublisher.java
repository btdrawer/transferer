package com.transferer.shared.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.transferer.shared.domain.events.DomainEvent;
import com.transferer.shared.events.EventPublisher;
import com.transferer.shared.events.TransactionalEventPublisher;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class OutboxEventPublisher implements EventPublisher, TransactionalEventPublisher {
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    
    public OutboxEventPublisher(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public Mono<Void> publish(DomainEvent<?> event) {
        return getOutboxEvent(event)
                .flatMap(outboxEventRepository::save)
                .then();
    }
    
    private Mono<String> serializeEvent(DomainEvent<?> event) {
        return Mono.fromCallable(() -> {
            try {
                return objectMapper.writeValueAsString(event.getBody());
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize event: " + event.getEventId(), e);
            }
        });
    }
    
    @Override
    public Mono<Void> publishWithinTransaction(List<DomainEvent<?>> events, TransactionalOperator transactionalOperator) {
        return Flux.fromIterable(events)
                .flatMap(this::getOutboxEvent)
                .flatMap(outboxEventRepository::save)
                .as(transactionalOperator::transactional)
                .then();
    }

    private Mono<OutboxEvent> getOutboxEvent(DomainEvent<?> event) {
        return serializeEvent(event)
                .map(eventBody ->
                        new OutboxEvent(
                                event.getEventId(),
                                event.getEventType(),
                                event.getAggregateId(),
                                eventBody,
                                event.getOccurredAt()
                        )
                );
    }
    
}
