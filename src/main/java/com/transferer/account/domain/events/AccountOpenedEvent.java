package com.transferer.account.domain.events;

import com.transferer.account.domain.AccountId;

import java.math.BigDecimal;

public class AccountOpenedEvent extends DomainEvent {
    private final AccountId accountId;
    private final String accountNumber;
    private final String holderName;
    private final BigDecimal initialBalance;

    public AccountOpenedEvent(AccountId accountId, String accountNumber, String holderName, BigDecimal initialBalance) {
        super("AccountOpened");
        this.accountId = accountId;
        this.accountNumber = accountNumber;
        this.holderName = holderName;
        this.initialBalance = initialBalance;
    }

    public AccountId getAccountId() {
        return accountId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getHolderName() {
        return holderName;
    }

    public BigDecimal getInitialBalance() {
        return initialBalance;
    }
}