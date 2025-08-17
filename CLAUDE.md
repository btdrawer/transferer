# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Development Commands

### Testing
- **Run all tests**: `mvn test`
- **Run single test**: `mvn test -Dtest=ClassName`
- **Run tests with specific method**: `mvn test -Dtest=ClassName#methodName`
- **Run integration tests**: `mvn test -Dtest=*IntegrationTest`

### Build and Run
- **Build project**: `mvn compile`
- **Package application**: `mvn package`
- **Run application**: `mvn spring-boot:run`
- **Clean build**: `mvn clean compile`

### Development
- **Install dependencies**: `mvn dependency:resolve`
- **View dependency tree**: `mvn dependency:tree`

## Architecture Overview

### Core Domain Structure
The application follows Domain-Driven Design (DDD) principles with a modular monolith architecture:

- **Account Module** (`com.transferer.account`): Manages user accounts with balance tracking
- **Payment Module** (`com.transferer.payment`): Handles payment processing using saga pattern
- **Transaction Module** (`com.transferer.transaction`): Core transaction management
- **Shared Module** (`com.transferer.shared`): Common domain events, outbox pattern, and infrastructure

### Event-Driven Architecture
- Uses **Outbox Pattern** for reliable event publishing via `OutboxEventBus`
- Domain events are stored in `outbox_events` table and processed asynchronously
- Events flow through reactive streams using Project Reactor
- Event types defined in `DomainEventType` enum with corresponding event bodies

### Payment Processing (Saga Pattern)
Payments use a multi-step saga pattern with compensation:
1. **INITIATED** → **TRANSACTION_CREATED** → **TRANSACTION_PROCESSING**
2. **SENDER_DEBITED** → **RECIPIENT_CREDITED** → **COMPLETED**
3. Compensation flow: **COMPENSATING_SENDER_CREDIT** → **COMPENSATED** if failures occur

### Data Layer
- **R2DBC** with reactive database access (H2 for dev, PostgreSQL for prod)
- Custom ID types: `AccountId`, `PaymentId`, `TransactionId` with UUID generation
- Repository pattern with R2DBC implementations
- Database schema in `src/main/resources/schema.sql`

### Testing Strategy
- Unit tests focus on domain logic validation
- Integration tests use `TestEventUtils` for outbox event verification
- TestContainers for database integration testing
- Separate test schema in `src/test/resources/test-schema.sql`

## Key Implementation Patterns

### Domain Events
All domain operations publish events via `OutboxEventBus`:
- Events are persisted to outbox table within the same transaction
- `TransactionalEventBus` ensures atomicity with business operations
- Use `TestEventUtils.performAndWaitForEvents()` in tests to verify event publication

### Error Handling
- Domain validation through entity constructors and business methods
- `GlobalExceptionHandler` for REST API error responses
- Compensation mechanisms in payment saga for failure scenarios

### Configuration Profiles
- **dev**: H2 in-memory database, debug logging
- **prod**: PostgreSQL, optimized connection pooling, info-level logging
- **test**: H2 with test schema, debug logging enabled

## Development Guidelines

### When Adding New Features
1. Start with domain entities and events in appropriate module
2. Implement repository interfaces with R2DBC
3. Add application services with event publishing
4. Create REST controllers with validation
5. Write tests using `TestEventUtils` for event verification

### Database Changes
- Update `schema.sql` for production schema
- Update `test-schema.sql` for test-specific changes
- Consider migration scripts for existing databases

### Event Design
- Create specific event types in `DomainEventType`
- Implement event body classes extending `DomainEventBody`
- Ensure events contain sufficient information for downstream processing