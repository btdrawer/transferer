package com.transferer.shared.outbox;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Repository
public interface OutboxEventRepository extends R2dbcRepository<OutboxEvent, Long> {
    
    @Query("SELECT * FROM outbox_events WHERE processed_at IS NULL ORDER BY occurred_at ASC LIMIT :limit")
    Flux<OutboxEvent> findUnprocessedEvents(int limit);
    
    @Query("UPDATE outbox_events SET processed_at = :processedAt WHERE id = :id")
    Mono<Integer> markAsProcessed(Long id, Instant processedAt);
    
    @Query("DELETE FROM outbox_events WHERE processed_at < :before")
    Mono<Integer> deleteProcessedEventsBefore(Instant before);
}