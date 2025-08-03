package com.transferer.shared.domain.events;

import com.transferer.shared.domain.events.body.DomainEventBody;

import java.time.Instant;
import java.util.UUID;

public abstract class DomainEvent<T extends DomainEventBody> {
    private final String eventId;
    private final Instant occurredAt;
    private final DomainEventType eventType;
    private final T body;

    protected DomainEvent(DomainEventType eventType, T body) {
        this.eventId = UUID.randomUUID().toString();
        this.occurredAt = Instant.now();
        this.eventType = eventType;
        this.body = body;
    }

    public String getEventId() {
        return eventId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public DomainEventType getEventType() {
        return eventType;
    }

    public T getBody() { return body; }

    public abstract String getAggregateId();
}
