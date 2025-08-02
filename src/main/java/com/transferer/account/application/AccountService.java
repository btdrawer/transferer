package com.transferer.account.application;

import com.transferer.account.domain.Account;
import com.transferer.account.domain.AccountId;
import com.transferer.account.domain.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Random;

@Service
@Transactional
public class AccountService {

    private final AccountRepository accountRepository;
    private final Random random = new Random();

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public Mono<Account> openAccount(String holderName, BigDecimal initialBalance) {
        if (holderName == null || holderName.trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException("Holder name cannot be null or empty"));
        }
        if (initialBalance == null || initialBalance.compareTo(BigDecimal.ZERO) < 0) {
            return Mono.error(new IllegalArgumentException("Initial balance cannot be null or negative"));
        }

        return generateUniqueAccountNumber()
                .map(accountNumber -> new Account(accountNumber, holderName.trim(), initialBalance))
                .flatMap(accountRepository::save);
    }

    @Transactional(readOnly = true)
    public Mono<Account> getAccount(AccountId accountId) {
        return accountRepository.findById(accountId)
                .switchIfEmpty(Mono.error(new AccountNotFoundException("Account not found with ID: " + accountId)));
    }

    @Transactional(readOnly = true)
    public Mono<Account> getAccountByNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .switchIfEmpty(Mono.error(new AccountNotFoundException("Account not found with number: " + accountNumber)));
    }

    @Transactional(readOnly = true)
    public Mono<BigDecimal> getAccountBalance(AccountId accountId) {
        return getAccount(accountId)
                .map(Account::getBalance);
    }

    public Mono<Account> creditAccount(AccountId accountId, BigDecimal amount) {
        return getAccount(accountId)
                .doOnNext(account -> account.credit(amount))
                .flatMap(accountRepository::save);
    }

    public Mono<Account> debitAccount(AccountId accountId, BigDecimal amount) {
        return getAccount(accountId)
                .doOnNext(account -> account.debit(amount))
                .flatMap(accountRepository::save);
    }

    public Mono<Account> suspendAccount(AccountId accountId) {
        return getAccount(accountId)
                .doOnNext(Account::suspend)
                .flatMap(accountRepository::save);
    }

    public Mono<Account> activateAccount(AccountId accountId) {
        return getAccount(accountId)
                .doOnNext(Account::activate)
                .flatMap(accountRepository::save);
    }

    public Mono<Account> deactivateAccount(AccountId accountId) {
        return getAccount(accountId)
                .doOnNext(Account::deactivate)
                .flatMap(accountRepository::save);
    }

    private Mono<String> generateUniqueAccountNumber() {
        return Mono.fromCallable(() -> String.format("%010d", random.nextInt(1_000_000_000)))
                .flatMap(accountNumber -> 
                    accountRepository.existsByAccountNumber(accountNumber)
                        .flatMap(exists -> exists ? generateUniqueAccountNumber() : Mono.just(accountNumber))
                );
    }
}