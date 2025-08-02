package com.transferer.account.domain.events;

import com.transferer.account.domain.AccountId;

public class AccountSuspendedEvent extends DomainEvent {
    private final AccountId accountId;
    private final String accountNumber;

    public AccountSuspendedEvent(AccountId accountId, String accountNumber) {
        super("AccountSuspended");
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