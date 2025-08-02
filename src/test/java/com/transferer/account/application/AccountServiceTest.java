package com.transferer.account.application;

import com.transferer.account.domain.Account;
import com.transferer.account.domain.AccountId;
import com.transferer.account.domain.AccountRepository;
import com.transferer.account.domain.AccountStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    private AccountService accountService;

    @BeforeEach
    void setUp() {
        accountService = new AccountService(accountRepository);
    }

    @Test
    void openAccount_WithValidData_ShouldCreateAccount() {
        when(accountRepository.existsByAccountNumber(anyString())).thenReturn(Mono.just(false));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        Mono<Account> result = accountService.openAccount("John Doe", BigDecimal.valueOf(1000));

        StepVerifier.create(result)
                .assertNext(account -> {
                    assertThat(account).isNotNull();
                    assertThat(account.getHolderName()).isEqualTo("John Doe");
                    assertThat(account.getBalance()).isEqualTo(BigDecimal.valueOf(1000));
                    assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
                })
                .verifyComplete();
        
        verify(accountRepository).save(any(Account.class));
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
        AccountId accountId = AccountId.generate();
        Account account = new Account("1234567890", "John Doe", BigDecimal.valueOf(1000));
        when(accountRepository.findById(accountId)).thenReturn(Mono.just(account));

        Mono<Account> result = accountService.getAccount(accountId);

        StepVerifier.create(result)
                .assertNext(returnedAccount -> assertThat(returnedAccount).isEqualTo(account))
                .verifyComplete();
    }

    @Test
    void getAccount_WithNonExistingId_ShouldThrowException() {
        AccountId accountId = AccountId.generate();
        when(accountRepository.findById(accountId)).thenReturn(Mono.empty());

        Mono<Account> result = accountService.getAccount(accountId);
        
        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof AccountNotFoundException &&
                        throwable.getMessage().equals("Account not found with ID: " + accountId))
                .verify();
    }

    @Test
    void creditAccount_WithValidAmount_ShouldIncreaseBalance() {
        AccountId accountId = AccountId.generate();
        Account account = new Account("1234567890", "John Doe", BigDecimal.valueOf(1000));
        when(accountRepository.findById(accountId)).thenReturn(Mono.just(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        Mono<Account> result = accountService.creditAccount(accountId, BigDecimal.valueOf(500));

        StepVerifier.create(result)
                .assertNext(updatedAccount -> assertThat(updatedAccount.getBalance()).isEqualTo(BigDecimal.valueOf(1500)))
                .verifyComplete();
        
        verify(accountRepository).save(account);
    }

    @Test
    void debitAccount_WithSufficientBalance_ShouldDecreaseBalance() {
        AccountId accountId = AccountId.generate();
        Account account = new Account("1234567890", "John Doe", BigDecimal.valueOf(1000));
        when(accountRepository.findById(accountId)).thenReturn(Mono.just(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        Mono<Account> result = accountService.debitAccount(accountId, BigDecimal.valueOf(300));

        StepVerifier.create(result)
                .assertNext(updatedAccount -> assertThat(updatedAccount.getBalance()).isEqualTo(BigDecimal.valueOf(700)))
                .verifyComplete();
        
        verify(accountRepository).save(account);
    }

    @Test
    void debitAccount_WithInsufficientBalance_ShouldThrowException() {
        AccountId accountId = AccountId.generate();
        Account account = new Account("1234567890", "John Doe", BigDecimal.valueOf(100));
        when(accountRepository.findById(accountId)).thenReturn(Mono.just(account));

        Mono<Account> result = accountService.debitAccount(accountId, BigDecimal.valueOf(500));
        
        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof IllegalArgumentException &&
                        throwable.getMessage().equals("Insufficient balance"))
                .verify();
    }
}