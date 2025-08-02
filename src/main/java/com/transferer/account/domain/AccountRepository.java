package com.transferer.account.domain;

import reactor.core.publisher.Mono;

public interface AccountRepository {
    
    Mono<Account> save(Account account);
    
    Mono<Account> findById(AccountId id);
    
    Mono<Account> findByAccountNumber(String accountNumber);
    
    Mono<Boolean> existsByAccountNumber(String accountNumber);
    
    Mono<Void> deleteById(AccountId id);
}