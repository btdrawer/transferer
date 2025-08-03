package com.transferer.transaction.domain.events.body;

import com.transferer.account.domain.AccountId;
import com.transferer.shared.domain.events.body.DomainEventBody;
import com.transferer.transaction.domain.TransactionId;

import java.math.BigDecimal;

public class TransactionFailedEventBody extends DomainEventBody {
    private final TransactionId transactionId;
    private final AccountId senderAccountId;
    private final AccountId recipientAccountId;
    private final BigDecimal amount;
    private final String failureReason;

    public TransactionFailedEventBody(
            TransactionId transactionId,
            AccountId senderAccountId,
            AccountId recipientAccountId,
            BigDecimal amount,
            String failureReason
    ) {
        this.transactionId = transactionId;
        this.senderAccountId = senderAccountId;
        this.recipientAccountId = recipientAccountId;
        this.amount = amount;
        this.failureReason = failureReason;
    }

    public TransactionId getTransactionId() {
        return transactionId;
    }

    public AccountId getSenderAccountId() {
        return senderAccountId;
    }

    public AccountId getRecipientAccountId() {
        return recipientAccountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getFailureReason() {
        return failureReason;
    }
}