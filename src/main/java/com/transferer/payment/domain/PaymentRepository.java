package com.transferer.payment.domain;

import com.transferer.account.domain.AccountId;
import com.transferer.shared.domain.events.DomainEvent;
import com.transferer.transaction.domain.TransactionId;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface PaymentRepository {
    Mono<Payment> save(Payment payment);
    
    Mono<Payment> findById(PaymentId id);
    
    Mono<Payment> findByTransactionId(TransactionId transactionId);

    Flux<Payment> findByAccountId(AccountId accountId);

    Flux<Payment> findByStatus(PaymentStatus status);
    
    Flux<Payment> findByCurrentStep(PaymentStep step);

    Mono<Void> deleteById(PaymentId id);

    Mono<Payment> saveAndPublishEvents(Payment payment, List<DomainEvent<?>> events);
}