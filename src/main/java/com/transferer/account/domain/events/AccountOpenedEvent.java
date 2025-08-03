package com.transferer.account.domain.events;

import com.transferer.account.domain.AccountId;
import com.transferer.account.domain.events.body.AccountOpenedEventBody;
import com.transferer.shared.domain.events.DomainEvent;
import com.transferer.shared.domain.events.DomainEventType;

import java.math.BigDecimal;

public class AccountOpenedEvent extends DomainEvent<AccountOpenedEventBody> {
    private final AccountId accountId;

    public AccountOpenedEvent(AccountId accountId, String accountNumber, String holderName, BigDecimal initialBalance) {
        super(
                DomainEventType.ACCOUNT_OPENED,
                new AccountOpenedEventBody(accountId, accountNumber, holderName, initialBalance)
        );
        this.accountId = accountId;
    }

    @Override
    public String getAggregateId() {
        return accountId.toString();
    }
}