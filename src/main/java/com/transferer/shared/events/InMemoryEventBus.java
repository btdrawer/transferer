package com.transferer.shared.events;

import com.transferer.shared.domain.events.DomainEvent;
import com.transferer.shared.domain.events.DomainEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

@Component
@ConditionalOnProperty(name = "eventbus.enabled", havingValue = "true", matchIfMissing = false)
public class InMemoryEventBus implements EventBus {
    
    private static final Logger logger = LoggerFactory.getLogger(InMemoryEventBus.class);
    
    private final Sinks.Many<DomainEvent<?>> eventSink;
    private final ConcurrentHashMap<Class<? extends DomainEvent<?>>, CopyOnWriteArrayList<Consumer<DomainEvent<?>>>> subscribers;
    
    public InMemoryEventBus() {
        this.eventSink = Sinks.many().multicast().onBackpressureBuffer();
        this.subscribers = new ConcurrentHashMap<>();
    }
    
    @Override
    public Mono<Void> publish(DomainEvent<?> event) {
        return Mono.fromRunnable(() -> {
            logger.info("Publishing event: {} with ID: {}", event.getEventType(), event.getEventId());

            // Emit to reactive stream
            eventSink.tryEmitNext(event);

            // Notify class-based subscribers
            CopyOnWriteArrayList<Consumer<DomainEvent<?>>> classHandlers = subscribers.get(event.getClass());
            if (classHandlers != null) {
                classHandlers.forEach(handler -> {
                    try {
                        handler.accept(event);
                    } catch (Exception e) {
                        logger.error("Error in event handler for class {}: {}", event.getClass().getSimpleName(), e.getMessage(), e);
                    }
                });
            }
        });
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
}
