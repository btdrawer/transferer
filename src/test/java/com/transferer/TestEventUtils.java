package com.transferer;

import com.transferer.shared.domain.events.DomainEvent;
import com.transferer.shared.domain.events.DomainEventType;
import com.transferer.shared.events.EventBus;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TestEventUtils {
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);

    /**
     * Combines an operation with verifying that specific event types were saved to the outbox table.
     * This is more appropriate for outbox pattern testing than waiting for reactive stream events.
     * 
     * @param operation the operation to perform (e.g., payment initiation)
     * @param databaseClient the database client to query the outbox table
     * @param expectedEventTypes the list of event types to verify in the outbox
     * @param timeout the maximum time to wait for events to appear in the outbox
     * @return Mono that completes with the operation result after expected events are verified in the outbox
     */
    public static <T> Mono<T> performAndWaitForEvents(
            Mono<T> operation, 
            DatabaseClient databaseClient, 
            List<DomainEventType> expectedEventTypes, 
            Duration timeout) {
        if (expectedEventTypes.isEmpty()) {
            return operation;
        }
        
        return operation
                .delayUntil(result -> waitForEventsInOutbox(databaseClient, expectedEventTypes, timeout));
    }

    /**
     * Combines an operation with verifying that specific event types were saved to the outbox table with default timeout.
     */
    public static <T> Mono<T> performAndWaitForEvents(Mono<T> operation, DatabaseClient databaseClient, List<DomainEventType> expectedEventTypes) {
        return performAndWaitForEvents(operation, databaseClient, expectedEventTypes, DEFAULT_TIMEOUT);
    }

    /**
     * Waits for specific event types to appear in the outbox_events table.
     * 
     * @param databaseClient the database client to query the outbox table
     * @param expectedEventTypes the list of event types to wait for
     * @return Mono that completes when all specified event types have been found in the outbox
     */
    public static Mono<Void> waitForEventTypes(DatabaseClient databaseClient, List<DomainEventType> expectedEventTypes) {
        return waitForEventsInOutbox(databaseClient, expectedEventTypes, DEFAULT_TIMEOUT);
    }

    /**
     * Waits for specific event types to appear in the outbox_events table with custom timeout.
     * 
     * @param databaseClient the database client to query the outbox table
     * @param expectedEventTypes the list of event types to wait for
     * @param timeout the maximum time to wait
     * @return Mono that completes when all specified event types have been found in the outbox
     */
    public static Mono<Void> waitForEventTypes(DatabaseClient databaseClient, List<DomainEventType> expectedEventTypes, Duration timeout) {
        return waitForEventsInOutbox(databaseClient, expectedEventTypes, timeout);
    }

    /**
     * Combines an operation with waiting for events to flow through the Kafka bridge.
     * This method waits for events to appear in both the outbox and be processed by the Kafka EventBus.
     * 
     * @param operation the operation to perform
     * @param databaseClient the database client to verify outbox events
     * @param eventBus the Kafka EventBus to monitor for processed events
     * @param expectedEventTypes the list of event types to wait for
     * @return Mono that completes with the operation result after events flow through the bridge
     */
    public static <T> Mono<T> performAndWaitForKafkaBridgeEvents(
            Mono<T> operation,
            DatabaseClient databaseClient,
            EventBus eventBus,
            List<DomainEventType> expectedEventTypes) {
        return performAndWaitForKafkaBridgeEvents(operation, databaseClient, eventBus, expectedEventTypes, DEFAULT_TIMEOUT);
    }

    /**
     * Combines an operation with waiting for events to flow through the Kafka bridge with custom timeout.
     */
    public static <T> Mono<T> performAndWaitForKafkaBridgeEvents(
            Mono<T> operation,
            DatabaseClient databaseClient,
            EventBus eventBus,
            List<DomainEventType> expectedEventTypes,
            Duration timeout) {
        if (expectedEventTypes.isEmpty()) {
            return operation;
        }

        // Track events received from Kafka
        Map<DomainEventType, Integer> receivedEventCounts = new ConcurrentHashMap<>();
        Map<DomainEventType, Integer> expectedCounts = new HashMap<>();
        
        for (DomainEventType eventType : expectedEventTypes) {
            expectedCounts.put(eventType, expectedCounts.getOrDefault(eventType, 0) + 1);
            receivedEventCounts.put(eventType, 0);
        }

        // Subscribe to events from Kafka EventBus
        for (DomainEventType eventType : expectedCounts.keySet()) {
            eventBus.eventStream(eventType)
                    .doOnNext(event -> {
                        receivedEventCounts.merge(eventType, 1, Integer::sum);
                    })
                    .subscribe();
        }

        return operation
                .delayUntil(result -> 
                    // First wait for events in outbox (bridge source)
                    waitForEventsInOutbox(databaseClient, expectedEventTypes, timeout)
                            // Then wait for events to flow through Kafka (bridge destination)
                            .then(waitForKafkaEvents(receivedEventCounts, expectedCounts, timeout))
                );
    }

    private static Mono<Void> waitForKafkaEvents(
            Map<DomainEventType, Integer> receivedEventCounts,
            Map<DomainEventType, Integer> expectedCounts,
            Duration timeout) {
        
        return Mono.defer(() -> {
            boolean allEventsReceived = expectedCounts.entrySet().stream()
                    .allMatch(entry -> {
                        DomainEventType eventType = entry.getKey();
                        int expectedCount = entry.getValue();
                        int receivedCount = receivedEventCounts.getOrDefault(eventType, 0);
                        return receivedCount >= expectedCount;
                    });

            if (allEventsReceived) {
                return Mono.empty();
            } else {
                return Mono.delay(Duration.ofMillis(100))
                        .then(waitForKafkaEvents(receivedEventCounts, expectedCounts, timeout));
            }
        }).timeout(timeout);
    }
    
    /**
     * Waits for specific event types to appear in the outbox_events table.
     * 
     * @param databaseClient the database client to query the outbox table
     * @param expectedEventTypes the list of event types to wait for
     * @param timeout the maximum time to wait
     * @return Mono that completes when all specified event types have been found in the outbox
     */
    public static Mono<Void> waitForEventsInOutbox(DatabaseClient databaseClient, List<DomainEventType> expectedEventTypes, Duration timeout) {
        if (expectedEventTypes.isEmpty()) {
            return Mono.empty();
        }
        
        // Count how many times each event type appears in the list
        Map<DomainEventType, Integer> expectedCounts = new HashMap<>();
        for (DomainEventType eventType : expectedEventTypes) {
            expectedCounts.put(eventType, expectedCounts.getOrDefault(eventType, 0) + 1);
        }
        
        return Mono.delay(Duration.ofMillis(100)) // Give some time for events to be saved
                .then(checkOutboxForEvents(databaseClient, expectedCounts))
                .timeout(timeout);
    }
    
    /**
     * Checks if the expected events are present in the outbox table.
     */
    private static Mono<Void> checkOutboxForEvents(DatabaseClient databaseClient, Map<DomainEventType, Integer> expectedCounts) {
        return databaseClient
                .sql("SELECT event_type, COUNT(*) as count FROM outbox_events GROUP BY event_type")
                .fetch()
                .all()
                .collectMap(
                    row -> DomainEventType.valueOf((String) row.get("event_type")),
                    row -> ((Number) row.get("count")).intValue()
                )
                .flatMap(actualCounts -> {
                    boolean allEventsFound = expectedCounts.entrySet().stream()
                            .allMatch(entry -> {
                                DomainEventType eventType = entry.getKey();
                                int expectedCount = entry.getValue();
                                int actualCount = actualCounts.getOrDefault(eventType, 0);
                                return actualCount >= expectedCount;
                            });
                    
                    if (allEventsFound) {
                        return Mono.empty();
                    } else {
                        // Retry after a short delay
                        return Mono.delay(Duration.ofMillis(50))
                                .then(checkOutboxForEvents(databaseClient, expectedCounts));
                    }
                });
    }
}