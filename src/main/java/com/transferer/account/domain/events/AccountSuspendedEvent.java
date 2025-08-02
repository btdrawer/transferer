package com.transferer.account.domain.events;

import com.transferer.account.domain.AccountId;
import com.transferer.account.domain.events.body.AccountSuspendedEventBody;
import com.transferer.shared.domain.events.DomainEvent;

public class AccountSuspendedEvent extends DomainEvent<AccountSuspendedEventBody> {
    private final AccountId accountId;

    public AccountSuspendedEvent(AccountId accountId, String accountNumber) {
        super(
                "AccountSuspended",
                new AccountSuspendedEventBody(accountId, accountNumber)
        );
        this.accountId = accountId;
    }

    @Override
    public String getAggregateId() {
        return accountId.toString();
    }
}