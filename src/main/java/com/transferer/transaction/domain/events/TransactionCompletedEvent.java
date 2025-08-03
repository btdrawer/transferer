package com.transferer.transaction.domain.events;

import com.transferer.account.domain.AccountId;
import com.transferer.shared.domain.events.DomainEvent;
import com.transferer.shared.domain.events.DomainEventType;
import com.transferer.transaction.domain.TransactionId;
import com.transferer.transaction.domain.events.body.TransactionCompletedEventBody;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionCompletedEvent extends DomainEvent<TransactionCompletedEventBody> {
    private final TransactionId transactionId;

    public TransactionCompletedEvent(
            TransactionId transactionId,
            AccountId senderAccountId,
            AccountId recipientAccountId,
            BigDecimal amount,
            LocalDateTime completedAt
    ) {
        super(
                DomainEventType.TRANSACTION_COMPLETED,
                new TransactionCompletedEventBody(transactionId, senderAccountId, recipientAccountId, amount, completedAt)
        );
        this.transactionId = transactionId;
    }

    @Override
    public String getAggregateId() {
        return transactionId.toString();
    }
}