package com.transferer.shared.events;

import com.transferer.shared.domain.events.DomainEvent;
import com.transferer.shared.domain.events.DomainEventType;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;

public interface EventBus extends EventPublisher {
    <T extends DomainEvent<?>> void subscribe(Class<T> eventClass, Consumer<T> handler);
    Flux<DomainEvent<?>> eventStream();
    Flux<DomainEvent<?>> eventStream(DomainEventType eventType);
}