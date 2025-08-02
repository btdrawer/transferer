package com.transferer.account.domain.events;

import com.transferer.account.domain.AccountId;

public class AccountActivatedEvent extends DomainEvent {
    private final AccountId accountId;
    private final String accountNumber;

    public AccountActivatedEvent(AccountId accountId, String accountNumber) {
        super("AccountActivated");
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