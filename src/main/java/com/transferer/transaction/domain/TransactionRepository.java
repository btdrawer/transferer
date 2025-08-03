package com.transferer.transaction.domain;

import com.transferer.account.domain.AccountId;
import com.transferer.shared.domain.events.DomainEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface TransactionRepository {
    Mono<Transaction> save(Transaction transaction);
    
    Mono<Transaction> findById(TransactionId id);
    
    Flux<Transaction> findBySenderAccountId(AccountId senderAccountId);
    
    Flux<Transaction> findByRecipientAccountId(AccountId recipientAccountId);
    
    Flux<Transaction> findByAccountId(AccountId accountId);
    
    Flux<Transaction> findByStatus(TransactionStatus status);
    
    Mono<Void> deleteById(TransactionId id);

    Mono<Transaction> saveAndPublishEvents(Transaction transaction, List<DomainEvent<?>> events);
}