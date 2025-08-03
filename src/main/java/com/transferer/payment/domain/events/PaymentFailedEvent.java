package com.transferer.payment.domain.events;

import com.transferer.account.domain.AccountId;
import com.transferer.payment.domain.PaymentId;
import com.transferer.payment.domain.PaymentStep;
import com.transferer.payment.domain.events.body.PaymentFailedEventBody;
import com.transferer.shared.domain.events.DomainEvent;
import com.transferer.shared.domain.events.DomainEventType;
import com.transferer.transaction.domain.TransactionId;

import java.math.BigDecimal;

public class PaymentFailedEvent extends DomainEvent<PaymentFailedEventBody> {
    private final PaymentId paymentId;

    public PaymentFailedEvent(
            PaymentId paymentId,
            TransactionId transactionId,
            AccountId senderAccountId,
            AccountId recipientAccountId,
            BigDecimal amount,
            PaymentStep failedAtStep,
            String failureReason
    ) {
        super(
                DomainEventType.PAYMENT_FAILED,
                new PaymentFailedEventBody(paymentId, transactionId, senderAccountId, recipientAccountId, amount, failedAtStep, failureReason)
        );
        this.paymentId = paymentId;
    }

    @Override
    public String getAggregateId() {
        return paymentId.toString();
    }
}