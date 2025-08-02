package com.transferer.account.domain.events;

import com.transferer.account.domain.AccountId;
import com.transferer.account.domain.events.body.AccountActivatedEventBody;
import com.transferer.shared.domain.events.DomainEvent;

public class AccountActivatedEvent extends DomainEvent<AccountActivatedEventBody> {
    private final AccountId accountId;

    public AccountActivatedEvent(AccountId accountId, String accountNumber) {
        super(
                "AccountActivated",
                new AccountActivatedEventBody(accountId, accountNumber)
        );
        this.accountId = accountId;
    }

    @Override
    public String getAggregateId() {
        return accountId.toString();
    }
}
