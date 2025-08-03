package com.transferer.payment.domain.events.body;

import com.transferer.payment.domain.PaymentId;
import com.transferer.payment.domain.PaymentStep;
import com.transferer.shared.domain.events.body.DomainEventBody;

import java.util.Optional;

public class PaymentStepAdvancedEventBody extends DomainEventBody {
    private final PaymentId paymentId;
    private final Optional<PaymentStep> previousStep;
    private final PaymentStep currentStep;

    public PaymentStepAdvancedEventBody(
            PaymentId paymentId,
            Optional<PaymentStep> previousStep,
            PaymentStep currentStep
    ) {
        this.paymentId = paymentId;
        this.previousStep = previousStep;
        this.currentStep = currentStep;
    }

    public PaymentId getPaymentId() {
        return paymentId;
    }

    public Optional<PaymentStep> getPreviousStep() {
        return previousStep;
    }

    public PaymentStep getCurrentStep() {
        return currentStep;
    }
}