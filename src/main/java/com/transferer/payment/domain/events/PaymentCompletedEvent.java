package com.transferer.payment.domain.events;

import com.transferer.account.domain.AccountId;
import com.transferer.payment.domain.PaymentId;
import com.transferer.payment.domain.events.body.PaymentCompletedEventBody;
import com.transferer.shared.domain.events.DomainEvent;
import com.transferer.shared.domain.events.DomainEventType;
import com.transferer.transaction.domain.TransactionId;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentCompletedEvent extends DomainEvent<PaymentCompletedEventBody> {
    private final PaymentId paymentId;

    public PaymentCompletedEvent(
            PaymentId paymentId,
            TransactionId transactionId,
            AccountId senderAccountId,
            AccountId recipientAccountId,
            BigDecimal amount,
            LocalDateTime completedAt
    ) {
        super(
                DomainEventType.PAYMENT_COMPLETED,
                new PaymentCompletedEventBody(paymentId, transactionId, senderAccountId, recipientAccountId, amount, completedAt)
        );
        this.paymentId = paymentId;
    }

    @Override
    public String getAggregateId() {
        return paymentId.toString();
    }
}