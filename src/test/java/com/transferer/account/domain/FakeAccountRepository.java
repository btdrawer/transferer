package com.transferer.account.domain;

import com.transferer.shared.domain.events.DomainEvent;
import com.transferer.shared.events.EventPublisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class FakeAccountRepository implements AccountRepository {
    private final Map<String, Account> accounts = new ConcurrentHashMap<>();
    private final EventPublisher eventPublisher;

    public FakeAccountRepository(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Mono<Account> save(Account account) {
        accounts.put(account.getIdValue(), account);
        return Mono.just(account);
    }

    @Override
    public Mono<Account> findById(AccountId id) {
        Account account = accounts.get(id.getValue());
        return account != null ? Mono.just(account) : Mono.empty();
    }

    @Override
    public Mono<Account> findByAccountNumber(String accountNumber) {
        return Mono.fromCallable(() -> accounts.values().stream()
                .filter(account -> account.getAccountNumber().equals(accountNumber))
                .findFirst()
                .orElse(null))
                .filter(account -> account != null);
    }

    @Override
    public Mono<Boolean> existsByAccountNumber(String accountNumber) {
        boolean exists = accounts.values().stream()
                .anyMatch(account -> account.getAccountNumber().equals(accountNumber));
        return Mono.just(exists);
    }

    @Override
    public Mono<Void> deleteById(AccountId id) {
        accounts.remove(id.getValue());
        return Mono.empty();
    }

    @Override
    public Mono<Account> saveAndPublishEvents(Account account, List<DomainEvent<?>> events) {
        return save(account)
                .flatMap(savedAccount ->
                        Flux.fromIterable(events)
                                .flatMap(eventPublisher::publish)
                                .then(Mono.just(savedAccount))
                );
    }

    public void clear() {
        accounts.clear();
    }

    public int size() {
        return accounts.size();
    }
}
