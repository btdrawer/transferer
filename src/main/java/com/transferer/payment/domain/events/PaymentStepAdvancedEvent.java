package com.transferer.payment.domain.events;

import com.transferer.payment.domain.PaymentId;
import com.transferer.payment.domain.PaymentStep;
import com.transferer.payment.domain.events.body.PaymentStepAdvancedEventBody;
import com.transferer.shared.domain.events.DomainEvent;
import com.transferer.shared.domain.events.DomainEventType;

import java.util.Optional;

public class PaymentStepAdvancedEvent extends DomainEvent<PaymentStepAdvancedEventBody> {
    private final PaymentId paymentId;

    public PaymentStepAdvancedEvent(
            PaymentId paymentId,
            Optional<PaymentStep> previousStep,
            PaymentStep currentStep
    ) {
        super(
                DomainEventType.PAYMENT_STEP_ADVANCED,
                new PaymentStepAdvancedEventBody(paymentId, previousStep, currentStep)
        );
        this.paymentId = paymentId;
    }

    @Override
    public String getAggregateId() {
        return paymentId.toString();
    }
}