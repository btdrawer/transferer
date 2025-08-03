package com.transferer.payment.domain.events.body;

import com.transferer.account.domain.AccountId;
import com.transferer.payment.domain.PaymentId;
import com.transferer.shared.domain.events.body.DomainEventBody;
import com.transferer.transaction.domain.TransactionId;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentCompletedEventBody extends DomainEventBody {
    private final PaymentId paymentId;
    private final TransactionId transactionId;
    private final AccountId senderAccountId;
    private final AccountId recipientAccountId;
    private final BigDecimal amount;
    private final LocalDateTime completedAt;

    public PaymentCompletedEventBody(
            PaymentId paymentId,
            TransactionId transactionId,
            AccountId senderAccountId,
            AccountId recipientAccountId,
            BigDecimal amount,
            LocalDateTime completedAt
    ) {
        this.paymentId = paymentId;
        this.transactionId = transactionId;
        this.senderAccountId = senderAccountId;
        this.recipientAccountId = recipientAccountId;
        this.amount = amount;
        this.completedAt = completedAt;
    }

    public PaymentId getPaymentId() {
        return paymentId;
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

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
}