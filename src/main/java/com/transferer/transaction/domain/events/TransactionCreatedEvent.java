package com.transferer.transaction.domain.events;

import com.transferer.account.domain.AccountId;
import com.transferer.shared.domain.events.DomainEvent;
import com.transferer.shared.domain.events.DomainEventType;
import com.transferer.transaction.domain.TransactionId;
import com.transferer.transaction.domain.events.body.TransactionCreatedEventBody;

import java.math.BigDecimal;

public class TransactionCreatedEvent extends DomainEvent<TransactionCreatedEventBody> {
    private final TransactionId transactionId;

    public TransactionCreatedEvent(
            TransactionId transactionId,
            AccountId senderAccountId,
            AccountId recipientAccountId,
            BigDecimal amount,
            String description
    ) {
        super(
                DomainEventType.TRANSACTION_CREATED,
                new TransactionCreatedEventBody(transactionId, senderAccountId, recipientAccountId, amount, description)
        );
        this.transactionId = transactionId;
    }

    @Override
    public String getAggregateId() {
        return transactionId.toString();
    }
}