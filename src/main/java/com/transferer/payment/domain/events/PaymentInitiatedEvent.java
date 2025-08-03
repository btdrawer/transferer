package com.transferer.payment.domain.events;

import com.transferer.account.domain.AccountId;
import com.transferer.payment.domain.PaymentId;
import com.transferer.payment.domain.events.body.PaymentInitiatedEventBody;
import com.transferer.shared.domain.events.DomainEvent;
import com.transferer.shared.domain.events.DomainEventType;

import java.math.BigDecimal;

public class PaymentInitiatedEvent extends DomainEvent<PaymentInitiatedEventBody> {
    private final PaymentId paymentId;

    public PaymentInitiatedEvent(
            PaymentId paymentId,
            AccountId senderAccountId,
            AccountId recipientAccountId,
            BigDecimal amount,
            String description
    ) {
        super(
                DomainEventType.PAYMENT_INITIATED,
                new PaymentInitiatedEventBody(paymentId, senderAccountId, recipientAccountId, amount, description)
        );
        this.paymentId = paymentId;
    }

    @Override
    public String getAggregateId() {
        return paymentId.toString();
    }
}