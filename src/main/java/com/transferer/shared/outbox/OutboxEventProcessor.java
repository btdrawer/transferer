package com.transferer.shared.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transferer.account.domain.events.DomainEvent;
import com.transferer.shared.events.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.Instant;

@Component
public class OutboxEventProcessor {
    private static final Logger logger = LoggerFactory.getLogger(OutboxEventProcessor.class);
    
    private final OutboxEventRepository outboxEventRepository;
    private final EventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final int batchSize;
    private final Duration cleanupRetention;
    
    public OutboxEventProcessor(
            OutboxEventRepository outboxEventRepository,
            EventPublisher eventPublisher,
            ObjectMapper objectMapper,
            @Value("${outbox.processor.batch-size:100}") int batchSize,
            @Value("${outbox.processor.cleanup-retention-days:7}") int cleanupRetentionDays) {
        this.outboxEventRepository = outboxEventRepository;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
        this.batchSize = batchSize;
        this.cleanupRetention = Duration.ofDays(cleanupRetentionDays);
    }
    
    @Scheduled(fixedDelayString = "${outbox.processor.interval:5000}")
    public void processOutboxEvents() {
        outboxEventRepository.findUnprocessedEvents(batchSize)
                .flatMap(this::processEvent)
                .onErrorResume(error -> {
                    logger.error("Error processing outbox events", error);
                    return Mono.empty();
                })
                .collectList()
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(
                    processedCount -> {
                        if (!processedCount.isEmpty()) {
                            logger.info("Processed {} outbox events", processedCount.size());
                        }
                    },
                    error -> logger.error("Unexpected error in outbox event processing", error)
                );
    }
    
    @Scheduled(fixedDelayString = "${outbox.processor.cleanup-interval:3600000}")
    public void cleanupProcessedEvents() {
        Instant cutoff = Instant.now().minus(cleanupRetention);
        outboxEventRepository.deleteProcessedEventsBefore(cutoff)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(
                    deletedCount -> {
                        if (deletedCount > 0) {
                            logger.info("Cleaned up {} processed outbox events older than {}", deletedCount, cutoff);
                        }
                    },
                    error -> logger.error("Error cleaning up processed outbox events", error)
                );
    }
    
    private Mono<OutboxEvent> processEvent(OutboxEvent outboxEvent) {
        return deserializeAndPublishEvent(outboxEvent)
                .then(markEventAsProcessed(outboxEvent))
                .onErrorResume(error -> {
                    logger.error("Failed to process outbox event: {} with ID: {}", 
                            outboxEvent.getEventType(), outboxEvent.getEventId(), error);
                    return Mono.empty();
                });
    }
    
    private Mono<Void> deserializeAndPublishEvent(OutboxEvent outboxEvent) {
        return Mono.fromCallable(() -> deserializeEvent(outboxEvent))
                .flatMap(eventPublisher::publish);
    }
    
    private DomainEvent deserializeEvent(OutboxEvent outboxEvent) throws Exception {
        return objectMapper.readValue(outboxEvent.getEventData(), DomainEvent.class);
    }
    
    private Mono<OutboxEvent> markEventAsProcessed(OutboxEvent outboxEvent) {
        return outboxEventRepository.markAsProcessed(outboxEvent.getId(), Instant.now())
                .thenReturn(outboxEvent);
    }
}