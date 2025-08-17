package com.transferer;

import com.transferer.shared.domain.events.DomainEvent;
import com.transferer.shared.domain.events.DomainEventType;
import com.transferer.shared.events.EventBus;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * Utility class for waiting for event processing in integration tests.
 */
public class TestEventUtils {
    
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);
    
    /**
     * Waits for a specific number of events to be processed by the EventBus.
     * 
     * @param eventBus the EventBus to monitor
     * @param expectedEventCount the number of events to wait for
     * @param timeout the maximum time to wait
     * @return Mono that completes when the expected number of events have been processed
     */
    public static Mono<Void> waitForEventCount(EventBus eventBus, int expectedEventCount, Duration timeout) {
        return eventBus.eventStream()
                .take(expectedEventCount)
                .then()
                .timeout(timeout);
    }
    
    /**
     * Waits for a specific number of events to be processed by the EventBus with default timeout.
     */
    public static Mono<Void> waitForEventCount(EventBus eventBus, int expectedEventCount) {
        return waitForEventCount(eventBus, expectedEventCount, DEFAULT_TIMEOUT);
    }
    
    /**
     * Waits for a specific event type to be processed by the EventBus.
     * 
     * @param eventBus the EventBus to monitor
     * @param eventType the type of event to wait for
     * @param timeout the maximum time to wait
     * @return Mono that completes when the event type has been processed
     */
    public static Mono<Void> waitForEventType(EventBus eventBus, DomainEventType eventType, Duration timeout) {
        return eventBus.eventStream(eventType)
                .next()
                .then()
                .timeout(timeout);
    }
    
    /**
     * Waits for a specific event type to be processed by the EventBus with default timeout.
     */
    public static Mono<Void> waitForEventType(EventBus eventBus, DomainEventType eventType) {
        return waitForEventType(eventBus, eventType, DEFAULT_TIMEOUT);
    }
    
    /**
     * Waits for an event that matches a specific predicate.
     * 
     * @param eventBus the EventBus to monitor
     * @param eventPredicate predicate to match the desired event
     * @param timeout the maximum time to wait
     * @return Mono that completes when a matching event has been processed
     */
    public static Mono<Void> waitForEvent(EventBus eventBus, Predicate<DomainEvent<?>> eventPredicate, Duration timeout) {
        return eventBus.eventStream()
                .filter(eventPredicate)
                .next()
                .then()
                .timeout(timeout);
    }
    
    /**
     * Waits for an event that matches a specific predicate with default timeout.
     */
    public static Mono<Void> waitForEvent(EventBus eventBus, Predicate<DomainEvent<?>> eventPredicate) {
        return waitForEvent(eventBus, eventPredicate, DEFAULT_TIMEOUT);
    }
    
    /**
     * Waits for all current event processing to complete by waiting for a short period.
     * 
     * @param eventBus the EventBus to monitor (for API consistency, but not used in this implementation)
     * @param timeout the maximum time to wait (for API consistency, but not used in this implementation)
     * @return Mono that completes when event processing appears to be finished
     */
    public static Mono<Void> waitForEventProcessingToComplete(EventBus eventBus, Duration timeout) {
        return Mono.delay(Duration.ofMillis(300)) // Wait 300ms for event processing to complete
                .then();
    }
    
    /**
     * Waits for all current event processing to complete with default timeout.
     */
    public static Mono<Void> waitForEventProcessingToComplete(EventBus eventBus) {
        return waitForEventProcessingToComplete(eventBus, DEFAULT_TIMEOUT);
    }
    
    /**
     * Combines an operation with waiting for events to be processed.
     * This method uses a simple delay to allow event processing to complete.
     * 
     * @param operation the operation to perform (e.g., payment initiation)
     * @param eventBus the EventBus to monitor (for API consistency, but not used in this implementation)
     * @param expectedEventCount the number of events expected from the operation (for API consistency, but not used in this implementation)
     * @param timeout the maximum time to wait for events (for API consistency, but not used in this implementation)
     * @return Mono that completes with the operation result after events are processed
     */
    public static <T> Mono<T> performAndWaitForEvents(
            Mono<T> operation, 
            EventBus eventBus, 
            int expectedEventCount, 
            Duration timeout) {
        
        // Perform the operation and then wait for a short delay to allow event processing
        return operation
                .delayElement(Duration.ofMillis(200)); // Wait 200ms for event processing to complete
    }
    
    /**
     * Combines an operation with waiting for events to be processed with default timeout.
     */
    public static <T> Mono<T> performAndWaitForEvents(Mono<T> operation, EventBus eventBus, int expectedEventCount) {
        return performAndWaitForEvents(operation, eventBus, expectedEventCount, DEFAULT_TIMEOUT);
    }
    
    /**
     * Simple blocking wait for testing scenarios where reactive patterns aren't practical.
     * 
     * @param eventBus the EventBus to monitor
     * @param expectedEventCount the number of events to wait for
     * @param timeoutSeconds the timeout in seconds
     * @throws InterruptedException if the wait is interrupted
     * @throws RuntimeException if the timeout is exceeded
     */
    public static void blockingWaitForEvents(EventBus eventBus, int expectedEventCount, int timeoutSeconds) 
            throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(expectedEventCount);
        
        eventBus.eventStream()
                .take(expectedEventCount)
                .subscribe(
                    event -> latch.countDown(),
                    error -> { /* Log error if needed */ },
                    () -> { /* Completion handler */ }
                );
        
        if (!latch.await(timeoutSeconds, TimeUnit.SECONDS)) {
            throw new RuntimeException("Timeout waiting for " + expectedEventCount + " events");
        }
    }
    
    /**
     * Simple blocking wait with default timeout.
     */
    public static void blockingWaitForEvents(EventBus eventBus, int expectedEventCount) throws InterruptedException {
        blockingWaitForEvents(eventBus, expectedEventCount, 5);
    }
}