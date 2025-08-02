package com.transferer.account.application;

import com.transferer.account.domain.Account;
import com.transferer.account.domain.AccountId;
import com.transferer.account.domain.AccountStatus;
import com.transferer.account.domain.FakeAccountRepository;
import com.transferer.account.domain.events.*;
import com.transferer.shared.events.FakeEventPublisher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

class AccountServiceTest {
    private FakeAccountRepository accountRepository;
    private FakeEventPublisher eventPublisher;
    private AccountService accountService;

    @BeforeEach
    void setUp() {
        eventPublisher = new FakeEventPublisher();
        accountRepository = new FakeAccountRepository(eventPublisher);
        accountService = new AccountService(accountRepository);
    }

    @AfterEach
    void tearDown() {
        accountRepository.clear();
        eventPublisher.clear();
    }

    @Test
    void openAccount_WithValidData_ShouldCreateAccount() {
        Mono<Account> result = accountService.openAccount("John Doe", BigDecimal.valueOf(1000));

        StepVerifier.create(result)
                .assertNext(account -> {
                    assertThat(account).isNotNull();
                    assertThat(account.getHolderName()).isEqualTo("John Doe");
                    assertThat(account.getBalance()).isEqualTo(BigDecimal.valueOf(1000));
                    assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
                })
                .verifyComplete();
        
        assertThat(accountRepository.size()).isEqualTo(1);
    }

    @Test
    void openAccount_WithNullHolderName_ShouldThrowException() {
        Mono<Account> result = accountService.openAccount(null, BigDecimal.valueOf(1000));
        
        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof IllegalArgumentException &&
                        throwable.getMessage().equals("Holder name cannot be null or empty"))
                .verify();
    }

    @Test
    void openAccount_WithNegativeBalance_ShouldThrowException() {
        Mono<Account> result = accountService.openAccount("John Doe", BigDecimal.valueOf(-100));
        
        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof IllegalArgumentException &&
                        throwable.getMessage().equals("Initial balance cannot be null or negative"))
                .verify();
    }

    @Test
    void getAccount_WithExistingId_ShouldReturnAccount() {
        Account account = new Account("1234567890", "John Doe", BigDecimal.valueOf(1000));
        accountRepository.save(account).block();
        AccountId accountId = account.getId();

        Mono<Account> result = accountService.getAccount(accountId);

        StepVerifier.create(result)
                .assertNext(returnedAccount -> assertThat(returnedAccount).isEqualTo(account))
                .verifyComplete();
    }

    @Test
    void getAccount_WithNonExistingId_ShouldThrowException() {
        AccountId accountId = AccountId.generate();

        Mono<Account> result = accountService.getAccount(accountId);
        
        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof AccountNotFoundException &&
                        throwable.getMessage().equals("Account not found with ID: " + accountId))
                .verify();
    }

    @Test
    void creditAccount_WithValidAmount_ShouldIncreaseBalance() {
        Account account = new Account("1234567890", "John Doe", BigDecimal.valueOf(1000));
        accountRepository.save(account).block();
        AccountId accountId = account.getId();

        Mono<Account> result = accountService.creditAccount(accountId, BigDecimal.valueOf(500));

        StepVerifier.create(result)
                .assertNext(updatedAccount -> assertThat(updatedAccount.getBalance()).isEqualTo(BigDecimal.valueOf(1500)))
                .verifyComplete();
    }

    @Test
    void debitAccount_WithSufficientBalance_ShouldDecreaseBalance() {
        Account account = new Account("1234567890", "John Doe", BigDecimal.valueOf(1000));
        accountRepository.save(account).block();
        AccountId accountId = account.getId();

        Mono<Account> result = accountService.debitAccount(accountId, BigDecimal.valueOf(300));

        StepVerifier.create(result)
                .assertNext(updatedAccount -> assertThat(updatedAccount.getBalance()).isEqualTo(BigDecimal.valueOf(700)))
                .verifyComplete();
    }

    @Test
    void debitAccount_WithInsufficientBalance_ShouldThrowException() {
        Account account = new Account("1234567890", "John Doe", BigDecimal.valueOf(100));
        accountRepository.save(account).block();
        AccountId accountId = account.getId();

        Mono<Account> result = accountService.debitAccount(accountId, BigDecimal.valueOf(500));
        
        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof IllegalArgumentException &&
                        throwable.getMessage().equals("Insufficient balance"))
                .verify();
    }

    @Test
    void openAccount_ShouldPublishAccountOpenedEvent() {
        Mono<Account> result = accountService.openAccount("John Doe", BigDecimal.valueOf(1000));

        StepVerifier.create(result)
                .assertNext(account -> {
                    assertThat(eventPublisher.getEventCount()).isEqualTo(1);
                    assertThat(eventPublisher.getEventsOfType(AccountOpenedEvent.class)).hasSize(1);
                    
                    AccountOpenedEvent event = eventPublisher.getEventsOfType(AccountOpenedEvent.class).get(0);
                    assertThat(event.getAccountId()).isEqualTo(account.getId());
                    assertThat(event.getAccountNumber()).isEqualTo(account.getAccountNumber());
                    assertThat(event.getHolderName()).isEqualTo("John Doe");
                    assertThat(event.getInitialBalance()).isEqualTo(BigDecimal.valueOf(1000));
                })
                .verifyComplete();
    }

    @Test
    void creditAccount_ShouldPublishAccountCreditedEvent() {
        Account account = new Account("1234567890", "John Doe", BigDecimal.valueOf(1000));
        accountRepository.save(account).block();
        AccountId accountId = account.getId();

        Mono<Account> result = accountService.creditAccount(accountId, BigDecimal.valueOf(500));

        StepVerifier.create(result)
                .assertNext(updatedAccount -> {
                    assertThat(eventPublisher.getEventCount()).isEqualTo(1);
                    assertThat(eventPublisher.getEventsOfType(AccountCreditedEvent.class)).hasSize(1);
                    
                    AccountCreditedEvent event = eventPublisher.getEventsOfType(AccountCreditedEvent.class).get(0);
                    assertThat(event.getAccountId()).isEqualTo(accountId);
                    assertThat(event.getAmount()).isEqualTo(BigDecimal.valueOf(500));
                    assertThat(event.getNewBalance()).isEqualTo(BigDecimal.valueOf(1500));
                })
                .verifyComplete();
    }

    @Test
    void debitAccount_ShouldPublishAccountDebitedEvent() {
        Account account = new Account("1234567890", "John Doe", BigDecimal.valueOf(1000));
        accountRepository.save(account).block();
        AccountId accountId = account.getId();

        Mono<Account> result = accountService.debitAccount(accountId, BigDecimal.valueOf(300));

        StepVerifier.create(result)
                .assertNext(updatedAccount -> {
                    assertThat(eventPublisher.getEventCount()).isEqualTo(1);
                    assertThat(eventPublisher.getEventsOfType(AccountDebitedEvent.class)).hasSize(1);
                    
                    AccountDebitedEvent event = eventPublisher.getEventsOfType(AccountDebitedEvent.class).get(0);
                    assertThat(event.getAccountId()).isEqualTo(accountId);
                    assertThat(event.getAmount()).isEqualTo(BigDecimal.valueOf(300));
                    assertThat(event.getNewBalance()).isEqualTo(BigDecimal.valueOf(700));
                })
                .verifyComplete();
    }

    @Test
    void activateAccount_ShouldPublishAccountActivatedEvent() {
        Account account = new Account("1234567890", "John Doe", BigDecimal.valueOf(1000));
        account.deactivate();
        accountRepository.save(account).block();
        AccountId accountId = account.getId();

        Mono<Account> result = accountService.activateAccount(accountId);

        StepVerifier.create(result)
                .assertNext(updatedAccount -> {
                    assertThat(eventPublisher.getEventCount()).isEqualTo(1);
                    assertThat(eventPublisher.getEventsOfType(AccountActivatedEvent.class)).hasSize(1);
                    
                    AccountActivatedEvent event = eventPublisher.getEventsOfType(AccountActivatedEvent.class).get(0);
                    assertThat(event.getAccountId()).isEqualTo(accountId);
                    assertThat(event.getAccountNumber()).isEqualTo(account.getAccountNumber());
                })
                .verifyComplete();
    }

    @Test
    void deactivateAccount_ShouldPublishAccountDeactivatedEvent() {
        Account account = new Account("1234567890", "John Doe", BigDecimal.valueOf(1000));
        accountRepository.save(account).block();
        AccountId accountId = account.getId();

        Mono<Account> result = accountService.deactivateAccount(accountId);

        StepVerifier.create(result)
                .assertNext(updatedAccount -> {
                    assertThat(eventPublisher.getEventCount()).isEqualTo(1);
                    assertThat(eventPublisher.getEventsOfType(AccountDeactivatedEvent.class)).hasSize(1);
                    
                    AccountDeactivatedEvent event = eventPublisher.getEventsOfType(AccountDeactivatedEvent.class).get(0);
                    assertThat(event.getAccountId()).isEqualTo(accountId);
                    assertThat(event.getAccountNumber()).isEqualTo(account.getAccountNumber());
                })
                .verifyComplete();
    }

    @Test
    void suspendAccount_ShouldPublishAccountSuspendedEvent() {
        Account account = new Account("1234567890", "John Doe", BigDecimal.valueOf(1000));
        accountRepository.save(account).block();
        AccountId accountId = account.getId();

        Mono<Account> result = accountService.suspendAccount(accountId);

        StepVerifier.create(result)
                .assertNext(updatedAccount -> {
                    assertThat(eventPublisher.getEventCount()).isEqualTo(1);
                    assertThat(eventPublisher.getEventsOfType(AccountSuspendedEvent.class)).hasSize(1);
                    
                    AccountSuspendedEvent event = eventPublisher.getEventsOfType(AccountSuspendedEvent.class).get(0);
                    assertThat(event.getAccountId()).isEqualTo(accountId);
                    assertThat(event.getAccountNumber()).isEqualTo(account.getAccountNumber());
                })
                .verifyComplete();
    }
}