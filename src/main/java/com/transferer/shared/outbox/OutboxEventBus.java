package com.transferer.shared.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.transferer.shared.domain.events.DomainEvent;
import com.transferer.shared.domain.events.DomainEventType;
import com.transferer.shared.events.TransactionalEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

@Component
@Primary
public class OutboxEventBus implements TransactionalEventBus {
    
    private static final Logger logger = LoggerFactory.getLogger(OutboxEventBus.class);
    
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final Sinks.Many<DomainEvent<?>> eventSink;
    private final ConcurrentHashMap<Class<? extends DomainEvent<?>>, CopyOnWriteArrayList<Consumer<DomainEvent<?>>>> subscribers;
    
    public OutboxEventBus(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
        this.eventSink = Sinks.many().multicast().onBackpressureBuffer();
        this.subscribers = new ConcurrentHashMap<>();
    }
    
    @Override
    public Mono<Void> publish(DomainEvent<?> event) {
        return getOutboxEvent(event)
                .flatMap(outboxEventRepository::save)
                .doOnNext(savedEvent -> {
                    // Emit to reactive stream after successful save
                    eventSink.tryEmitNext(event);
                    notifySubscribers(event);
                })
                .then();
    }

    // TODO do not publish to handlers; a separate process should scan the table
    @Override
    public Mono<Void> publishWithinTransaction(List<DomainEvent<?>> events, TransactionalOperator transactionalOperator) {
        return Flux.fromIterable(events)
                .flatMap(event -> 
                    getOutboxEvent(event)
                            .flatMap(outboxEventRepository::save)
                            .doOnNext(savedEvent -> {
                                // Emit the original event to reactive stream after successful save
                                eventSink.tryEmitNext(event);
                                notifySubscribers(event);
                            })
                            .then(Mono.just(event))
                )
                .as(transactionalOperator::transactional)
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
        return eventSink.asFlux().filter(event -> event.getEventType().equals(eventType));
    }
    
    private void notifySubscribers(DomainEvent<?> event) {
        // Notify class-based subscribers
        CopyOnWriteArrayList<Consumer<DomainEvent<?>>> handlers = subscribers.get(event.getClass());
        if (handlers != null) {
            handlers.forEach(handler -> {
                try {
                    handler.accept(event);
                } catch (Exception e) {
                    logger.error("Error in event handler for class {}: {}", event.getClass().getSimpleName(), e.getMessage(), e);
                }
            });
        }
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