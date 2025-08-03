package com.transferer.payment.domain.events.body;

import com.transferer.account.domain.AccountId;
import com.transferer.payment.domain.PaymentId;
import com.transferer.payment.domain.PaymentStep;
import com.transferer.shared.domain.events.body.DomainEventBody;
import com.transferer.transaction.domain.TransactionId;

import java.math.BigDecimal;

public class PaymentFailedEventBody extends DomainEventBody {
    private final PaymentId paymentId;
    private final TransactionId transactionId;
    private final AccountId senderAccountId;
    private final AccountId recipientAccountId;
    private final BigDecimal amount;
    private final PaymentStep failedAtStep;
    private final String failureReason;

    public PaymentFailedEventBody(
            PaymentId paymentId,
            TransactionId transactionId,
            AccountId senderAccountId,
            AccountId recipientAccountId,
            BigDecimal amount,
            PaymentStep failedAtStep,
            String failureReason
    ) {
        this.paymentId = paymentId;
        this.transactionId = transactionId;
        this.senderAccountId = senderAccountId;
        this.recipientAccountId = recipientAccountId;
        this.amount = amount;
        this.failedAtStep = failedAtStep;
        this.failureReason = failureReason;
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

    public PaymentStep getFailedAtStep() {
        return failedAtStep;
    }

    public String getFailureReason() {
        return failureReason;
    }
}