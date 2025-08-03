package com.transferer.shared.events;

import com.transferer.shared.domain.events.DomainEvent;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.util.List;

public interface TransactionalEventBus extends EventBus {
    Mono<Void> publishWithinTransaction(
            List<DomainEvent<?>> events,
            TransactionalOperator transactionalOperator
    );
}