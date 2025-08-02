package com.transferer.shared.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.transferer.account.domain.events.DomainEvent;
import com.transferer.shared.events.EventPublisher;
import com.transferer.shared.events.TransactionalEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@Primary
public class OutboxEventPublisher implements EventPublisher, TransactionalEventPublisher {
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    
    public OutboxEventPublisher(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public Mono<Void> publish(DomainEvent event) {
        return getOutboxEvent(event)
                .flatMap(outboxEventRepository::save)
                .then();
    }
    
    private Mono<String> serializeEvent(DomainEvent event) {
        return Mono.fromCallable(() -> {
            try {
                // TODO only serialize body
                return objectMapper.writeValueAsString(event);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize event: " + event.getEventId(), e);
            }
        });
    }
    
    @Override
    public Mono<Void> publishWithinTransaction(List<DomainEvent> events, TransactionalOperator transactionalOperator) {
        return Flux.fromIterable(events)
                .flatMap(this::getOutboxEvent)
                .flatMap(outboxEventRepository::save)
                .as(transactionalOperator::transactional)
                .then();
    }

    private Mono<OutboxEvent> getOutboxEvent(DomainEvent event) {
        return serializeEvent(event)
                .map(eventData ->
                        new OutboxEvent(
                                event.getEventId(),
                                event.getEventType(),
                                extractAggregateId(event),
                                eventData,
                                event.getOccurredAt()
                        )
                );
    }
    
    private String extractAggregateId(DomainEvent event) {
        if (event instanceof com.transferer.account.domain.events.AccountOpenedEvent) {
            return ((com.transferer.account.domain.events.AccountOpenedEvent) event).getAccountId().toString();
        } else if (event instanceof com.transferer.account.domain.events.AccountCreditedEvent) {
            return ((com.transferer.account.domain.events.AccountCreditedEvent) event).getAccountId().toString();
        } else if (event instanceof com.transferer.account.domain.events.AccountDebitedEvent) {
            return ((com.transferer.account.domain.events.AccountDebitedEvent) event).getAccountId().toString();
        } else if (event instanceof com.transferer.account.domain.events.AccountSuspendedEvent) {
            return ((com.transferer.account.domain.events.AccountSuspendedEvent) event).getAccountId().toString();
        } else if (event instanceof com.transferer.account.domain.events.AccountActivatedEvent) {
            return ((com.transferer.account.domain.events.AccountActivatedEvent) event).getAccountId().toString();
        } else if (event instanceof com.transferer.account.domain.events.AccountDeactivatedEvent) {
            return ((com.transferer.account.domain.events.AccountDeactivatedEvent) event).getAccountId().toString();
        }
        return "unknown";
    }
}
