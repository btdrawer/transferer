package com.transferer.shared.events;

import com.transferer.account.domain.events.DomainEvent;
import reactor.core.publisher.Mono;

public interface EventPublisher {
    Mono<Void> publish(DomainEvent event);
}