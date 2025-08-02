package com.transferer.account.domain.events;

import com.transferer.account.domain.AccountId;

import java.math.BigDecimal;

public class AccountDebitedEvent extends DomainEvent {
    private final AccountId accountId;
    private final String accountNumber;
    private final BigDecimal amount;
    private final BigDecimal newBalance;

    public AccountDebitedEvent(AccountId accountId, String accountNumber, BigDecimal amount, BigDecimal newBalance) {
        super("AccountDebited");
        this.accountId = accountId;
        this.accountNumber = accountNumber;
        this.amount = amount;
        this.newBalance = newBalance;
    }

    public AccountId getAccountId() {
        return accountId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getNewBalance() {
        return newBalance;
    }
}