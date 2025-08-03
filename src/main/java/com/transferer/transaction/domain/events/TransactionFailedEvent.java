package com.transferer.transaction.domain.events;

import com.transferer.account.domain.AccountId;
import com.transferer.shared.domain.events.DomainEvent;
import com.transferer.shared.domain.events.DomainEventType;
import com.transferer.transaction.domain.TransactionId;
import com.transferer.transaction.domain.events.body.TransactionFailedEventBody;

import java.math.BigDecimal;

public class TransactionFailedEvent extends DomainEvent<TransactionFailedEventBody> {
    private final TransactionId transactionId;

    public TransactionFailedEvent(
            TransactionId transactionId,
            AccountId senderAccountId,
            AccountId recipientAccountId,
            BigDecimal amount,
            String failureReason
    ) {
        super(
                DomainEventType.TRANSACTION_FAILED,
                new TransactionFailedEventBody(transactionId, senderAccountId, recipientAccountId, amount, failureReason)
        );
        this.transactionId = transactionId;
    }

    @Override
    public String getAggregateId() {
        return transactionId.toString();
    }
}