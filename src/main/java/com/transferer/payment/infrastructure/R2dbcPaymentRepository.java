package com.transferer.payment.infrastructure;

import com.transferer.account.domain.AccountId;
import com.transferer.payment.domain.Payment;
import com.transferer.payment.domain.PaymentId;
import com.transferer.payment.domain.PaymentRepository;
import com.transferer.payment.domain.PaymentStatus;
import com.transferer.payment.domain.PaymentStep;
import com.transferer.shared.domain.events.DomainEvent;
import com.transferer.shared.events.TransactionalEventPublisher;
import com.transferer.transaction.domain.TransactionId;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

interface R2dbcPaymentRepositoryJpa extends R2dbcRepository<Payment, PaymentId> {
    Mono<Payment> findByTransactionId(TransactionId transactionId);
    Flux<Payment> findBySenderAccountId(AccountId senderAccountId);
    Flux<Payment> findByRecipientAccountId(AccountId recipientAccountId);
    Flux<Payment> findByStatus(PaymentStatus status);
    Flux<Payment> findByCurrentStep(PaymentStep step);
    
    @Query("SELECT * FROM payments WHERE sender_account_id = :accountId OR recipient_account_id = :accountId")
    Flux<Payment> findByAccountId(AccountId accountId);
    
    @Query("SELECT * FROM payments WHERE status = 'COMPENSATING' OR (status = 'PROCESSING' AND current_step IN ('SENDER_DEBITED', 'RECIPIENT_CREDITED'))")
    Flux<Payment> findPendingCompensations();
}

@Repository
public class R2dbcPaymentRepository implements PaymentRepository {
    private final R2dbcPaymentRepositoryJpa jpaRepository;
    private final TransactionalEventPublisher eventPublisher;
    private final TransactionalOperator transactionalOperator;
    
    public R2dbcPaymentRepository(
            R2dbcPaymentRepositoryJpa jpaRepository,
            TransactionalEventPublisher eventPublisher,
            TransactionalOperator transactionalOperator) {
        this.jpaRepository = jpaRepository;
        this.eventPublisher = eventPublisher;
        this.transactionalOperator = transactionalOperator;
    }
    
    @Override
    public Mono<Payment> save(Payment payment) {
        return jpaRepository.save(payment);
    }

    @Override
    public Mono<Payment> saveAndPublishEvents(Payment payment, List<DomainEvent<?>> events) {
        return jpaRepository.save(payment)
                .flatMap(savedPayment ->
                        eventPublisher.publishWithinTransaction(events, transactionalOperator)
                                .then(Mono.just(savedPayment))
                )
                .as(transactionalOperator::transactional);
    }
    
    @Override
    public Mono<Payment> findById(PaymentId id) {
        return jpaRepository.findById(id);
    }
    
    @Override
    public Mono<Payment> findByTransactionId(TransactionId transactionId) {
        return jpaRepository.findByTransactionId(transactionId);
    }
    
    @Override
    public Flux<Payment> findBySenderAccountId(AccountId senderAccountId) {
        return jpaRepository.findBySenderAccountId(senderAccountId);
    }
    
    @Override
    public Flux<Payment> findByRecipientAccountId(AccountId recipientAccountId) {
        return jpaRepository.findByRecipientAccountId(recipientAccountId);
    }
    
    @Override
    public Flux<Payment> findByAccountId(AccountId accountId) {
        return jpaRepository.findByAccountId(accountId);
    }
    
    @Override
    public Flux<Payment> findByStatus(PaymentStatus status) {
        return jpaRepository.findByStatus(status);
    }
    
    @Override
    public Flux<Payment> findByCurrentStep(PaymentStep step) {
        return jpaRepository.findByCurrentStep(step);
    }
    
    @Override
    public Flux<Payment> findPendingCompensations() {
        return jpaRepository.findPendingCompensations();
    }
    
    @Override
    public Mono<Void> deleteById(PaymentId id) {
        return jpaRepository.deleteById(id);
    }
}