package com.transferer.transaction.infrastructure;

import com.transferer.account.domain.AccountId;
import com.transferer.shared.domain.events.DomainEvent;
import com.transferer.shared.events.TransactionalEventPublisher;
import com.transferer.transaction.domain.Transaction;
import com.transferer.transaction.domain.TransactionId;
import com.transferer.transaction.domain.TransactionRepository;
import com.transferer.transaction.domain.TransactionStatus;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

interface R2dbcTransactionRepositoryJpa extends R2dbcRepository<Transaction, TransactionId> {
    Flux<Transaction> findBySenderAccountId(AccountId senderAccountId);
    Flux<Transaction> findByRecipientAccountId(AccountId recipientAccountId);
    Flux<Transaction> findByStatus(TransactionStatus status);
    
    @Query("SELECT * FROM transactions WHERE sender_account_id = :accountId OR recipient_account_id = :accountId")
    Flux<Transaction> findByAccountId(AccountId accountId);
}

@Repository
public class R2dbcTransactionRepository implements TransactionRepository {
    private final R2dbcTransactionRepositoryJpa jpaRepository;
    private final TransactionalEventPublisher eventPublisher;
    private final TransactionalOperator transactionalOperator;
    
    public R2dbcTransactionRepository(
            R2dbcTransactionRepositoryJpa jpaRepository,
            TransactionalEventPublisher eventPublisher,
            TransactionalOperator transactionalOperator) {
        this.jpaRepository = jpaRepository;
        this.eventPublisher = eventPublisher;
        this.transactionalOperator = transactionalOperator;
    }
    
    @Override
    public Mono<Transaction> save(Transaction transaction) {
        return jpaRepository.save(transaction);
    }

    @Override
    public Mono<Transaction> saveAndPublishEvents(Transaction transaction, List<DomainEvent<?>> events) {
        return jpaRepository.save(transaction)
                .flatMap(savedTransaction ->
                        eventPublisher.publishWithinTransaction(events, transactionalOperator)
                                .then(Mono.just(savedTransaction))
                )
                .as(transactionalOperator::transactional);
    }
    
    @Override
    public Mono<Transaction> findById(TransactionId id) {
        return jpaRepository.findById(id);
    }
    
    @Override
    public Flux<Transaction> findBySenderAccountId(AccountId senderAccountId) {
        return jpaRepository.findBySenderAccountId(senderAccountId);
    }
    
    @Override
    public Flux<Transaction> findByRecipientAccountId(AccountId recipientAccountId) {
        return jpaRepository.findByRecipientAccountId(recipientAccountId);
    }
    
    @Override
    public Flux<Transaction> findByAccountId(AccountId accountId) {
        return jpaRepository.findByAccountId(accountId);
    }
    
    @Override
    public Flux<Transaction> findByStatus(TransactionStatus status) {
        return jpaRepository.findByStatus(status);
    }
    
    @Override
    public Mono<Void> deleteById(TransactionId id) {
        return jpaRepository.deleteById(id);
    }
}