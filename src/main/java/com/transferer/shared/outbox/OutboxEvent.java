package com.transferer.shared.outbox;

import com.transferer.shared.domain.events.DomainEventType;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Table("outbox_events")
public class OutboxEvent {
    
    @Id
    private Long id;
    
    @Column("event_id")
    private String eventId;
    
    @Column("event_type")
    private DomainEventType eventType;
    
    @Column("aggregate_id")
    private String aggregateId;
    
    @Column("event_data")
    private String eventData;
    
    @Column("occurred_at")
    private Instant occurredAt;
    
    @Column("processed_at")
    private Instant processedAt;
    
    @Column("created_at")
    private Instant createdAt;

    public OutboxEvent(String eventId, DomainEventType eventType, String aggregateId, String eventData, Instant occurredAt) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.aggregateId = aggregateId;
        this.eventData = eventData;
        this.occurredAt = occurredAt;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public DomainEventType getEventType() {
        return eventType;
    }

    public void setEventType(DomainEventType eventType) {
        this.eventType = eventType;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(String aggregateId) {
        this.aggregateId = aggregateId;
    }

    public String getEventData() {
        return eventData;
    }

    public void setEventData(String eventData) {
        this.eventData = eventData;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}