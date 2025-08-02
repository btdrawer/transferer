package com.transferer.shared.events;

import com.transferer.shared.domain.events.DomainEvent;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.util.List;

public interface TransactionalEventPublisher extends EventPublisher {
    Mono<Void> publishWithinTransaction(
            List<DomainEvent<?>> event,
            TransactionalOperator transactionalOperator
    );
}
