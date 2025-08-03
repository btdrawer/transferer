package com.transferer.shared.events;

import com.transferer.shared.domain.events.DomainEvent;
import com.transferer.shared.domain.events.DomainEventType;
import com.transferer.shared.domain.events.body.DomainEventBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EventBusTest {

    private InMemoryEventBus eventBus;

    @BeforeEach
    void setUp() {
        eventBus = new InMemoryEventBus();
    }
    
    @Test
    void shouldNotifyTypeSubscribers() throws InterruptedException {
        TestDomainEvent event = new TestDomainEvent();
        CountDownLatch latch = new CountDownLatch(1);
        List<DomainEvent<?>> receivedEvents = new ArrayList<>();
        
        eventBus.subscribe(TestDomainEventType.TEST_EVENT, receivedEvent -> {
            receivedEvents.add(receivedEvent);
            latch.countDown();
        });
        
        eventBus.publish(event).block();
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(1, receivedEvents.size());
        assertEquals(event, receivedEvents.get(0));
    }
    
    @Test
    void shouldNotifyClassSubscribers() throws InterruptedException {
        TestDomainEvent event = new TestDomainEvent();
        CountDownLatch latch = new CountDownLatch(1);
        List<DomainEvent<?>> receivedEvents = new ArrayList<>();
        
        eventBus.subscribe(TestDomainEvent.class, receivedEvent -> {
            receivedEvents.add(receivedEvent);
            latch.countDown();
        });
        
        eventBus.publish(event).block();
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(1, receivedEvents.size());
        assertEquals(event, receivedEvents.get(0));
    }
    
    @Test
    void shouldProvideEventStream() {
        TestDomainEvent event1 = new TestDomainEvent();
        TestDomainEvent event2 = new TestDomainEvent();
        
        Flux<DomainEvent<?>> eventStream = eventBus.eventStream();
        
        StepVerifier.create(eventStream.take(2))
                .then(() -> {
                    eventBus.publish(event1).block();
                    eventBus.publish(event2).block();
                })
                .expectNext(event1)
                .expectNext(event2)
                .verifyComplete();
    }
    
    @Test
    void shouldProvideFilteredEventStream() {
        TestDomainEvent testEvent = new TestDomainEvent();
        AnotherTestDomainEvent anotherEvent = new AnotherTestDomainEvent();
        
        Flux<DomainEvent<?>> eventStream = eventBus.eventStream(TestDomainEventType.TEST_EVENT);
        
        StepVerifier.create(eventStream.take(1))
                .then(() -> {
                    eventBus.publish(anotherEvent).block(); // Should be filtered out
                    eventBus.publish(testEvent).block(); // Should pass through
                })
                .expectNext(testEvent)
                .verifyComplete();
    }
    
    @Test
    void shouldHandleSubscriberExceptions() throws InterruptedException {
        TestDomainEvent event = new TestDomainEvent();
        CountDownLatch latch = new CountDownLatch(1);
        
        eventBus.subscribe(TestDomainEventType.TEST_EVENT, receivedEvent -> {
            throw new RuntimeException("Test exception");
        });
        
        eventBus.subscribe(TestDomainEventType.TEST_EVENT, receivedEvent -> {
            latch.countDown();
        });
        
        eventBus.publish(event).block();
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
    }
    
    // Test event classes
    enum TestDomainEventType implements DomainEventType {
        TEST_EVENT,
        ANOTHER_TEST_EVENT
    }
    
    static class TestEventBody implements DomainEventBody {
        public String data = "test";
    }
    
    static class TestDomainEvent extends DomainEvent<TestEventBody> {
        public TestDomainEvent() {
            super(TestDomainEventType.TEST_EVENT, new TestEventBody());
        }
        
        @Override
        public String getAggregateId() {
            return "test-aggregate";
        }
    }
    
    static class AnotherTestDomainEvent extends DomainEvent<TestEventBody> {
        public AnotherTestDomainEvent() {
            super(TestDomainEventType.ANOTHER_TEST_EVENT, new TestEventBody());
        }
        
        @Override
        public String getAggregateId() {
            return "another-test-aggregate";
        }
    }
}