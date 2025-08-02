package com.transferer.account.infrastructure;

import com.transferer.account.domain.Account;
import com.transferer.account.domain.AccountId;
import com.transferer.account.domain.AccountRepository;
import com.transferer.account.domain.events.DomainEvent;
import com.transferer.shared.events.TransactionalEventPublisher;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Repository
@Primary
public class TransactionalR2dbcAccountRepository implements AccountRepository {
    
    private final R2dbcAccountRepository baseRepository;
    private final TransactionalEventPublisher eventPublisher;
    private final TransactionalOperator transactionalOperator;
    
    public TransactionalR2dbcAccountRepository(
            R2dbcAccountRepository baseRepository,
            TransactionalEventPublisher eventPublisher,
            TransactionalOperator transactionalOperator) {
        this.baseRepository = baseRepository;
        this.eventPublisher = eventPublisher;
        this.transactionalOperator = transactionalOperator;
    }
    
    @Override
    public Mono<Account> saveAndPublishEvents(Account account, List<DomainEvent> events) {
        return baseRepository.save(account)
                .flatMap(savedAccount ->
                        eventPublisher.publishWithinTransaction(events, transactionalOperator)
                        .then(Mono.just(savedAccount))
                )
                .as(transactionalOperator::transactional);
    }
    
    @Override
    public Mono<Account> save(Account account) {
        return baseRepository.save(account);
    }
    
    @Override
    public Mono<Account> findById(AccountId id) {
        return baseRepository.findById(id);
    }
    
    @Override
    public Mono<Account> findByAccountNumber(String accountNumber) {
        return baseRepository.findByAccountNumber(accountNumber);
    }
    
    @Override
    public Mono<Boolean> existsByAccountNumber(String accountNumber) {
        return baseRepository.existsByAccountNumber(accountNumber);
    }
    
    @Override
    public Mono<Void> deleteById(AccountId id) {
        return baseRepository.deleteById(id);
    }
}