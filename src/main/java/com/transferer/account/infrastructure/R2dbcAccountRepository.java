package com.transferer.account.infrastructure;

import com.transferer.account.domain.Account;
import com.transferer.account.domain.AccountId;
import com.transferer.account.domain.AccountRepository;
import com.transferer.shared.domain.events.DomainEvent;
import com.transferer.shared.events.TransactionalEventPublisher;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.util.List;

interface R2dbcAccountRepositoryJpa extends R2dbcRepository<Account, AccountId> {
    Mono<Account> findByAccountNumber(String accountNumber);
    Mono<Boolean> existsByAccountNumber(String accountNumber);
}

@Repository
public class R2dbcAccountRepository implements AccountRepository {
    private final R2dbcAccountRepositoryJpa jpaRepository;
    private final TransactionalEventPublisher eventPublisher;
    private final TransactionalOperator transactionalOperator;
    
    public R2dbcAccountRepository(
            R2dbcAccountRepositoryJpa jpaRepository,
            TransactionalEventPublisher eventPublisher,
            TransactionalOperator transactionalOperator) {
        this.jpaRepository = jpaRepository;
        this.eventPublisher = eventPublisher;
        this.transactionalOperator = transactionalOperator;
    }
    
    @Override
    public Mono<Account> save(Account account) {
        return jpaRepository.save(account);
    }

    @Override
    public Mono<Account> saveAndPublishEvents(Account account, List<DomainEvent<?>> events) {
        return jpaRepository.save(account)
                .flatMap(savedAccount ->
                        eventPublisher.publishWithinTransaction(events, transactionalOperator)
                                .then(Mono.just(savedAccount))
                )
                .as(transactionalOperator::transactional);
    }
    
    @Override
    public Mono<Account> findById(AccountId id) {
        return jpaRepository.findById(id);
    }
    
    @Override
    public Mono<Account> findByAccountNumber(String accountNumber) {
        return jpaRepository.findByAccountNumber(accountNumber);
    }
    
    @Override
    public Mono<Boolean> existsByAccountNumber(String accountNumber) {
        return jpaRepository.existsByAccountNumber(accountNumber);
    }
    
    @Override
    public Mono<Void> deleteById(AccountId id) {
        return jpaRepository.deleteById(id);
    }
}