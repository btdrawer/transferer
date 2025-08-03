package com.transferer.account.domain.events;

import com.transferer.account.domain.AccountId;
import com.transferer.account.domain.events.body.AccountDeactivatedEventBody;
import com.transferer.shared.domain.events.DomainEvent;
import com.transferer.shared.domain.events.DomainEventType;

public class AccountDeactivatedEvent extends DomainEvent<AccountDeactivatedEventBody> {
    private final AccountId accountId;

    public AccountDeactivatedEvent(AccountId accountId, String accountNumber) {
        super(
                DomainEventType.ACCOUNT_DEACTIVATED,
                new AccountDeactivatedEventBody(accountId, accountNumber)
        );
        this.accountId = accountId;
    }

    @Override
    public String getAggregateId() {
        return accountId.toString();
    }
}