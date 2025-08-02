package com.transferer.account.domain;

import reactor.core.publisher.Mono;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class FakeAccountRepository implements AccountRepository {
    private final Map<String, Account> accounts = new ConcurrentHashMap<>();

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

    public void clear() {
        accounts.clear();
    }

    public int size() {
        return accounts.size();
    }
}
