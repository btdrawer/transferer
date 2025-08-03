package com.transferer.account.domain.events;

import com.transferer.account.domain.AccountId;
import com.transferer.account.domain.events.body.AccountDebitedEventBody;
import com.transferer.shared.domain.events.DomainEvent;
import com.transferer.shared.domain.events.DomainEventType;
import com.transferer.transaction.domain.TransactionId;

import java.math.BigDecimal;

public class AccountDebitedEvent extends DomainEvent<AccountDebitedEventBody> {
    private final AccountId accountId;

    public AccountDebitedEvent(
            AccountId accountId,
            TransactionId transactionId,
            String accountNumber,
            BigDecimal amount,
            BigDecimal newBalance
    ) {
        super(
                DomainEventType.ACCOUNT_DEBITED,
                new AccountDebitedEventBody(accountId, transactionId, accountNumber, amount, newBalance)
        );
        this.accountId = accountId;
    }

    @Override
    public String getAggregateId() {
        return accountId.toString();
    }
}
