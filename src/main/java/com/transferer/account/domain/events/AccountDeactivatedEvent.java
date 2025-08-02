package com.transferer.account.domain.events;

import com.transferer.account.domain.AccountId;

public class AccountDeactivatedEvent extends DomainEvent {
    private final AccountId accountId;
    private final String accountNumber;

    public AccountDeactivatedEvent(AccountId accountId, String accountNumber) {
        super("AccountDeactivated");
        this.accountId = accountId;
        this.accountNumber = accountNumber;
    }

    public AccountId getAccountId() {
        return accountId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }
}