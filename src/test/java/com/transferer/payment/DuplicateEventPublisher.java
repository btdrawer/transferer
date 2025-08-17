package com.transferer.payment;

import com.transferer.shared.domain.events.DomainEvent;
import com.transferer.shared.domain.events.DomainEventType;
import com.transferer.shared.events.EventBus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

/**
 * A decorator EventBus that publishes each event twice to verify idempotency.
 * Each event is published, then immediately published again to ensure
 * the system handles duplicate events correctly.
 */
public class DuplicateEventPublisher implements EventBus {
    
    private final EventBus delegate;
    
    public DuplicateEventPublisher(EventBus delegate) {
        this.delegate = delegate;
    }
    
    @Override
    public Mono<Void> publish(DomainEvent<?> event) {
        return delegate.publish(event)
                .then(delegate.publish(event));
    }

    @Override
    public <T extends DomainEvent<?>> void subscribe(Class<T> eventClass, Consumer<T> handler) {
        delegate.subscribe(eventClass, handler);
    }

    @Override
    public Flux<DomainEvent<?>> eventStream() {
        return delegate.eventStream();
    }

    @Override
    public Flux<DomainEvent<?>> eventStream(DomainEventType eventType) {
        return delegate.eventStream(eventType);
    }
}