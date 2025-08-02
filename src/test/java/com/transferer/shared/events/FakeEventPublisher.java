package com.transferer.shared.events;

import com.transferer.account.domain.events.DomainEvent;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

public class FakeEventPublisher implements EventPublisher {
    private final List<DomainEvent> publishedEvents = new ArrayList<>();

    @Override
    public Mono<Void> publish(DomainEvent event) {
        publishedEvents.add(event);
        return Mono.empty();
    }

    public List<DomainEvent> getPublishedEvents() {
        return new ArrayList<>(publishedEvents);
    }

    public void clear() {
        publishedEvents.clear();
    }

    public int getEventCount() {
        return publishedEvents.size();
    }

    @SuppressWarnings("unchecked")
    public <T extends DomainEvent> List<T> getEventsOfType(Class<T> eventType) {
        return publishedEvents.stream()
                .filter(eventType::isInstance)
                .map(event -> (T) event)
                .toList();
    }
}