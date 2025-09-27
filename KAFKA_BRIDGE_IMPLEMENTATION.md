# Kafka Bridge Implementation

This document describes the Kafka-based event bridge implementation that has been added to the transferer payment service.

## Overview

The implementation provides a bridge between the existing outbox pattern and Kafka, allowing events to flow from the reliable outbox storage through Kafka to consuming services. This enables a hybrid approach where:

1. **Outbox Pattern**: Ensures transactional consistency and reliability for local event storage
2. **Kafka Bridge**: Streams events from outbox to Kafka for external service consumption
3. **Kafka EventBus**: Allows services to consume events directly from Kafka

## Components Implemented

### 1. KafkaEventBus (`src/main/java/com/transferer/shared/events/KafkaEventBus.java`)
- Implements the `EventBus` interface using Kafka as the transport
- Publishes events to Kafka topics with comprehensive headers
- Subscribes to events from Kafka with reactive streams
- Configurable via `transferer.eventbus.type=kafka`

### 2. OutboxToKafkaEventBridge (`src/main/java/com/transferer/shared/events/OutboxToKafkaEventBridge.java`)
- Subscribes to the OutboxEventBus event stream
- Publishes events to Kafka with serialized JSON payloads
- Adds metadata headers (eventType, eventId, aggregateId, source)
- Configurable via `transferer.eventbus.bridge.enabled=true`

### 3. Configuration Classes
- **KafkaEventBusConfiguration**: Configures Kafka producer/consumer for direct Kafka EventBus
- **OutboxToKafkaBridgeConfiguration**: Configures optimized Kafka producer for the bridge
- **KafkaBridgeTestConfiguration**: Test configuration with TestContainers Kafka

### 4. Enhanced TestEventUtils
- Added `performAndWaitForKafkaBridgeEvents()` methods
- Waits for events in both outbox (source) and Kafka (destination)
- Verifies end-to-end event flow through the bridge

## Architecture Patterns

### Pattern 1: Pure Outbox (Default)
```
Service -> OutboxEventBus -> Database -> Local Subscribers
```
Configuration: `transferer.eventbus.type=outbox`

### Pattern 2: Pure Kafka
```
Service -> KafkaEventBus -> Kafka -> Kafka Subscribers
```
Configuration: `transferer.eventbus.type=kafka`

### Pattern 3: Outbox-to-Kafka Bridge (Hybrid)
```
Service -> OutboxEventBus -> Database -> Bridge -> Kafka -> Kafka Subscribers
```
Configuration: 
```yaml
transferer:
  eventbus:
    type: outbox
    bridge:
      enabled: true
```

## Updated Payment Saga Tests

The `PaymentSagaIntegrationTest` and `PaymentSagaIdempotencyTest` have been updated to use the Kafka bridge pattern:

1. **KafkaBridgeTestConfiguration** provides TestContainers Kafka setup
2. **Bridge Initialization** starts the outbox-to-Kafka bridge in test setup
3. **Enhanced Event Verification** waits for events to flow through the complete pipeline
4. **Kafka EventBus Integration** services consume events from Kafka instead of outbox

## Usage Examples

### Enable Kafka Bridge
```yaml
transferer:
  eventbus:
    type: outbox
    bridge:
      enabled: true
  kafka:
    topic:
      events: domain-events
    consumer:
      group-id: transferer-events

spring:
  kafka:
    bootstrap-servers: localhost:9092
```

### Test Event Flow
```java
// Wait for events to flow through outbox -> Kafka -> services
TestEventUtils.performAndWaitForKafkaBridgeEvents(
    paymentService.initiatePayment(...),
    databaseClient,
    kafkaEventBus,
    Arrays.asList(DomainEventType.PAYMENT_INITIATED, DomainEventType.PAYMENT_STEP_ADVANCED)
)
```

## Benefits

1. **Reliability**: Maintains outbox pattern guarantees for local consistency
2. **Scalability**: Kafka enables horizontal scaling of event consumers
3. **Flexibility**: Can switch between outbox-only, Kafka-only, or hybrid modes
4. **Integration**: External services can consume events via Kafka
5. **Monitoring**: Kafka provides built-in monitoring and observability

## Dependencies Added

```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
<dependency>
    <groupId>io.projectreactor.kafka</groupId>
    <artifactId>reactor-kafka</artifactId>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>kafka</artifactId>
    <scope>test</scope>
</dependency>
```

## Testing

The implementation includes:
- Compilation verification
- Unit test compatibility 
- Integration test framework (requires Docker for full TestContainers execution)
- Enhanced test utilities for bridge verification

Note: Full integration tests require Docker to be available for TestContainers Kafka instances.