package com.transferer.account.domain.events;

import com.transferer.account.domain.AccountId;
import com.transferer.account.domain.events.body.AccountCreditEventBody;
import com.transferer.shared.domain.events.DomainEvent;
import com.transferer.shared.domain.events.DomainEventType;

import java.math.BigDecimal;

public class AccountCreditedEvent extends DomainEvent<AccountCreditEventBody> {
    private final AccountId accountId;

    public AccountCreditedEvent(
            AccountId accountId,
            String accountNumber,
            BigDecimal amount,
            BigDecimal newBalance
    ) {
        super(
                DomainEventType.ACCOUNT_CREDITED,
                new AccountCreditEventBody(accountId, accountNumber, amount, newBalance)
        );
        this.accountId = accountId;
    }

    @Override
    public String getAggregateId() {
        return accountId.toString();
    }
}
