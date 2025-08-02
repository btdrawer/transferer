package com.transferer.account.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

class AccountTest {
    @Test
    void createAccount_WithValidData_ShouldCreateAccount() {
        Account account = new Account("1234567890", "John Doe", BigDecimal.valueOf(1000));

        assertThat(account.getId()).isNotNull();
        assertThat(account.getAccountNumber()).isEqualTo("1234567890");
        assertThat(account.getHolderName()).isEqualTo("John Doe");
        assertThat(account.getBalance()).isEqualTo(BigDecimal.valueOf(1000));
        assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(account.getCreatedAt()).isNotNull();
        assertThat(account.getUpdatedAt()).isNotNull();
    }

    @Test
    void createAccount_WithNegativeBalance_ShouldThrowException() {
        assertThatThrownBy(() -> new Account("1234567890", "John Doe", BigDecimal.valueOf(-100)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Initial balance cannot be negative");
    }

    @Test
    void credit_WithPositiveAmount_ShouldIncreaseBalance() {
        Account account = new Account("1234567890", "John Doe", BigDecimal.valueOf(1000));

        account.credit(BigDecimal.valueOf(500));

        assertThat(account.getBalance()).isEqualTo(BigDecimal.valueOf(1500));
    }

    @Test
    void credit_WithZeroAmount_ShouldThrowException() {
        Account account = new Account("1234567890", "John Doe", BigDecimal.valueOf(1000));

        assertThatThrownBy(() -> account.credit(BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Credit amount must be positive");
    }

    @Test
    void debit_WithSufficientBalance_ShouldDecreaseBalance() {
        Account account = new Account("1234567890", "John Doe", BigDecimal.valueOf(1000));

        account.debit(BigDecimal.valueOf(300));

        assertThat(account.getBalance()).isEqualTo(BigDecimal.valueOf(700));
    }

    @Test
    void debit_WithInsufficientBalance_ShouldThrowException() {
        Account account = new Account("1234567890", "John Doe", BigDecimal.valueOf(100));

        assertThatThrownBy(() -> account.debit(BigDecimal.valueOf(500)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Insufficient balance");
    }

    @Test
    void suspend_ShouldChangeStatusToSuspended() {
        Account account = new Account("1234567890", "John Doe", BigDecimal.valueOf(1000));

        account.suspend();

        assertThat(account.getStatus()).isEqualTo(AccountStatus.SUSPENDED);
    }

    @Test
    void credit_OnSuspendedAccount_ShouldThrowException() {
        Account account = new Account("1234567890", "John Doe", BigDecimal.valueOf(1000));
        account.suspend();

        assertThatThrownBy(() -> account.credit(BigDecimal.valueOf(100)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot credit inactive account");
    }

    @Test
    void debit_OnSuspendedAccount_ShouldThrowException() {
        Account account = new Account("1234567890", "John Doe", BigDecimal.valueOf(1000));
        account.suspend();

        assertThatThrownBy(() -> account.debit(BigDecimal.valueOf(100)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot debit inactive account");
    }
}