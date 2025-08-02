package com.transferer.account.domain;

import com.transferer.account.domain.events.DomainEvent;
import reactor.core.publisher.Mono;

import java.util.List;

public interface AccountRepository {
    Mono<Account> save(Account account);
    
    Mono<Account> findById(AccountId id);
    
    Mono<Account> findByAccountNumber(String accountNumber);
    
    Mono<Boolean> existsByAccountNumber(String accountNumber);
    
    Mono<Void> deleteById(AccountId id);

    Mono<Account> saveAndPublishEvents(Account account, List<DomainEvent> events);
}