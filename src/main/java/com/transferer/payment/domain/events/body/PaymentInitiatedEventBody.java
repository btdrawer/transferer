package com.transferer.payment.domain.events.body;

import com.transferer.account.domain.AccountId;
import com.transferer.payment.domain.PaymentId;
import com.transferer.shared.domain.events.body.DomainEventBody;

import java.math.BigDecimal;

public class PaymentInitiatedEventBody extends DomainEventBody {
    private final PaymentId paymentId;
    private final AccountId senderAccountId;
    private final AccountId recipientAccountId;
    private final BigDecimal amount;
    private final String description;

    public PaymentInitiatedEventBody(
            PaymentId paymentId,
            AccountId senderAccountId,
            AccountId recipientAccountId,
            BigDecimal amount,
            String description
    ) {
        this.paymentId = paymentId;
        this.senderAccountId = senderAccountId;
        this.recipientAccountId = recipientAccountId;
        this.amount = amount;
        this.description = description;
    }

    public PaymentId getPaymentId() {
        return paymentId;
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

    public String getDescription() {
        return description;
    }
}