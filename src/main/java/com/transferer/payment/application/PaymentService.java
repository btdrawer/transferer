package com.transferer.payment.application;

import com.transferer.account.application.AccountService;
import com.transferer.account.domain.AccountId;
import com.transferer.account.domain.events.AccountCreditedEvent;
import com.transferer.account.domain.events.AccountDebitedEvent;
import com.transferer.payment.domain.Payment;
import com.transferer.payment.domain.PaymentId;
import com.transferer.payment.domain.PaymentRepository;
import com.transferer.payment.domain.PaymentStatus;
import com.transferer.payment.domain.PaymentStep;
import com.transferer.payment.domain.events.PaymentInitiatedEvent;
import com.transferer.payment.domain.events.PaymentCompletedEvent;
import com.transferer.payment.domain.events.PaymentFailedEvent;
import com.transferer.payment.domain.events.PaymentStepAdvancedEvent;
import com.transferer.shared.events.EventBus;
import com.transferer.transaction.application.TransactionService;
import com.transferer.transaction.domain.TransactionId;
import com.transferer.transaction.domain.events.TransactionCompletedEvent;
import com.transferer.transaction.domain.events.TransactionCreatedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final TransactionService transactionService;
    private final AccountService accountService;

    public PaymentService(
            PaymentRepository paymentRepository,
            TransactionService transactionService,
            AccountService accountService,
            EventBus eventBus
    ) {
        this.paymentRepository = paymentRepository;
        this.transactionService = transactionService;
        this.accountService = accountService;

        eventBus.subscribe(
                PaymentStepAdvancedEvent.class,
                this::handlePaymentStepAdvancedEvent
        );

        eventBus.subscribe(
                TransactionCreatedEvent.class,
                this::handleTransactionCreatedEvent
        );

        eventBus.subscribe(
                AccountDebitedEvent.class,
                this::handleAccountDebitedEvent
        );

        eventBus.subscribe(
                AccountCreditedEvent.class,
                this::handleAccountCreditedEvent
        );

        eventBus.subscribe(
                TransactionCompletedEvent.class,
                this::handleTransactionCompletedEvent
        );

        eventBus.subscribe(
                PaymentFailedEvent.class,
                this::handlePaymentFailedEvent
        );
    }

    private void handlePaymentStepAdvancedEvent(PaymentStepAdvancedEvent event) {
        PaymentId paymentId = event.getBody().getPaymentId();
        paymentRepository.findById(paymentId)
                .flatMap(this::processPaymentSaga)
                .subscribe();
    }

    private void handleTransactionCreatedEvent(TransactionCreatedEvent event) {
        TransactionId transactionId = event.getBody().getTransactionId();
        paymentRepository.findByTransactionId(transactionId)
                .flatMap(payment ->
                        savePaymentWithStepAdvancement(
                                payment,
                                PaymentStep.INITIATED,
                                PaymentStep.TRANSACTION_CREATED
                        )
                )
                .subscribe();
    }

    private void handleAccountDebitedEvent(AccountDebitedEvent event) {
        TransactionId transactionId = event.getBody().getTransactionId();
        paymentRepository.findByTransactionId(transactionId)
                .flatMap(payment ->
                        savePaymentWithStepAdvancement(
                                payment,
                                PaymentStep.TRANSACTION_PROCESSING,
                                PaymentStep.SENDER_DEBITED
                        )
                )
                .subscribe();
    }

    private void handleAccountCreditedEvent(AccountCreditedEvent event) {
        TransactionId transactionId = event.getBody().getTransactionId();
        paymentRepository.findByTransactionId(transactionId)
                .flatMap(payment -> {
                    if (payment.requiresCompensation()) {
                        PaymentStep currentStep = payment.getCurrentStep();
                        payment.markAsCompensated();
                        PaymentStepAdvancedEvent stepAdvancedEvent = new PaymentStepAdvancedEvent(
                                payment.getId(),
                                Optional.of(currentStep),
                                payment.getCurrentStep()
                        );
                        return Mono.fromRunnable(payment::markAsCompensated)
                                .then(
                                        paymentRepository.saveAndPublishEvents(payment, List.of(stepAdvancedEvent))
                                );
                    } else {
                        return savePaymentWithStepAdvancement(
                                payment,
                                PaymentStep.SENDER_DEBITED,
                                PaymentStep.RECIPIENT_CREDITED
                        );
                    }
                })
                .subscribe();
    }

    private void handleTransactionCompletedEvent(TransactionCompletedEvent event) {
        TransactionId transactionId = event.getBody().getTransactionId();
        paymentRepository.findByTransactionId(transactionId)
                .flatMap(payment -> {
                    PaymentStep currentStep = payment.getCurrentStep();
                    payment.markAsCompleted();
                    PaymentStepAdvancedEvent stepAdvancedEvent = new PaymentStepAdvancedEvent(
                            payment.getId(),
                            Optional.of(currentStep),
                            payment.getCurrentStep()
                    );
                    PaymentCompletedEvent completedEvent = new PaymentCompletedEvent(
                            payment.getId(),
                            payment.getTransactionId(),
                            payment.getSenderAccountId(),
                            payment.getRecipientAccountId(),
                            payment.getAmount(),
                            payment.getCompletedAt()
                    );
                    return paymentRepository.saveAndPublishEvents(
                            payment,
                            List.of(stepAdvancedEvent, completedEvent)
                    );
                })
                .subscribe();
    }

    private void handlePaymentFailedEvent(PaymentFailedEvent event) {
        PaymentId paymentId = event.getBody().getPaymentId();
        paymentRepository.findById(paymentId)
                .flatMap(savedPayment ->
                        transactionService.markTransactionAsFailed(
                                event.getBody().getTransactionId(),
                                event.getBody().getFailureReason()
                        ).then(Mono.just(savedPayment))
                );
    }

    public Mono<Payment> initiatePayment(AccountId senderAccountId, AccountId recipientAccountId, BigDecimal amount, String description) {
        if (senderAccountId == null) {
            return Mono.error(new IllegalArgumentException("Sender account ID cannot be null"));
        }
        if (recipientAccountId == null) {
            return Mono.error(new IllegalArgumentException("Recipient account ID cannot be null"));
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return Mono.error(new IllegalArgumentException("Amount must be positive"));
        }

        return Mono.fromCallable(() -> new Payment(senderAccountId, recipientAccountId, amount, description))
                .flatMap(payment -> {
                    // TODO consider building a way to feed changes and get back the events you need from them
                    PaymentInitiatedEvent initiatedEvent = new PaymentInitiatedEvent(
                            payment.getId(),
                            payment.getSenderAccountId(),
                            payment.getRecipientAccountId(),
                            payment.getAmount(),
                            payment.getDescription()
                    );
                    PaymentStepAdvancedEvent stepAdvancedEvent = new PaymentStepAdvancedEvent(
                            payment.getId(),
                            Optional.empty(),
                            payment.getCurrentStep()
                    );
                    return paymentRepository.saveAndPublishEvents(
                            payment,
                            List.of(initiatedEvent, stepAdvancedEvent)
                    );
                });
    }

    private Mono<Payment> processPaymentSaga(Payment payment) {
        return switch (payment.getCurrentStep()) {
            case INITIATED -> createTransaction(payment);
            case TRANSACTION_CREATED -> startTransactionProcessing(payment);
            case TRANSACTION_PROCESSING -> debitSenderAccount(payment);
            case SENDER_DEBITED -> creditRecipientAccount(payment);
            case RECIPIENT_CREDITED -> completePayment(payment);
            case COMPENSATING_SENDER_CREDIT -> compensateSenderAccount(payment);
            default -> Mono.just(payment);
        };
    }

    private Mono<Payment> createTransaction(Payment payment) {
        return transactionService.createTransaction(
                    payment.getSenderAccountId(),
                    payment.getRecipientAccountId(),
                    payment.getAmount(),
                    payment.getDescription()
                )
                .then(Mono.just(payment))
                .onErrorResume(error ->
                        handlePaymentFailure(
                                payment,
                                payment.getCurrentStep(),
                                error.getMessage()
                        )
                );
    }

    private Mono<Payment> startTransactionProcessing(Payment payment) {
        return transactionService.markTransactionAsProcessing(payment.getTransactionId())
                .then(
                        savePaymentWithStepAdvancement(
                                payment,
                                PaymentStep.TRANSACTION_CREATED,
                                PaymentStep.TRANSACTION_PROCESSING
                        )
                )
                .onErrorResume(error ->
                        handlePaymentFailure(
                                payment,
                                PaymentStep.TRANSACTION_CREATED,
                                error.getMessage()
                        )
                );
    }

    private Mono<Payment> debitSenderAccount(Payment payment) {
        return accountService.debitAccount(
                        payment.getSenderAccountId(),
                        payment.getTransactionId(),
                        payment.getAmount()
                )
                .then(Mono.just(payment))
                .onErrorResume(error ->
                        handlePaymentFailure(
                                payment,
                                PaymentStep.TRANSACTION_PROCESSING,
                                error.getMessage()
                        )
                );
    }

    private Mono<Payment> creditRecipientAccount(Payment payment) {
        return accountService.creditAccount(payment.getRecipientAccountId(), payment.getTransactionId(), payment.getAmount())
                .then(Mono.just(payment))
                .onErrorResume(error -> startCompensation(payment, error.getMessage()));
    }

    private Mono<Payment> completePayment(Payment payment) {
        return transactionService.markTransactionAsCompleted(payment.getTransactionId())
                .then(Mono.just(payment))
                .onErrorResume(error -> startCompensation(payment, error.getMessage()));
    }

    private Mono<Payment> startCompensation(Payment payment, String failureReason) {
        if (!payment.requiresCompensation()) {
            return handlePaymentFailure(payment, payment.getCurrentStep(), failureReason);
        }
        payment.startCompensation();
        return paymentRepository.save(payment);
    }

    private Mono<Payment> compensateSenderAccount(Payment payment) {
        return accountService.creditAccount(payment.getSenderAccountId(), payment.getTransactionId(), payment.getAmount())
                .then(Mono.just(payment))
                .onErrorResume(compensationError ->
                        handlePaymentFailure(payment, payment.getCurrentStep(),
                                "Compensation failed: " + compensationError.getMessage())
                );
    }

    private Mono<Payment> handlePaymentFailure(Payment payment, PaymentStep failedAtStep, String failureReason) {
        PaymentStep currentStep = payment.getCurrentStep();
        payment.markAsFailed(failureReason);

        PaymentFailedEvent failedEvent = new PaymentFailedEvent(
                payment.getId(),
                payment.getTransactionId(),
                payment.getSenderAccountId(),
                payment.getRecipientAccountId(),
                payment.getAmount(),
                failedAtStep,
                failureReason
        );
        PaymentStepAdvancedEvent stepAdvancedEvent = new PaymentStepAdvancedEvent(
                payment.getId(),
                Optional.of(currentStep),
                payment.getCurrentStep()
        );

        return paymentRepository.saveAndPublishEvents(payment, List.of(failedEvent, stepAdvancedEvent));
    }

    private Mono<Payment> savePaymentWithStepAdvancement(
            Payment payment,
            PaymentStep previousStep,
            PaymentStep newStep
    ) {
        payment.advanceToStep(newStep);
        
        PaymentStepAdvancedEvent event = new PaymentStepAdvancedEvent(
                payment.getId(),
                Optional.of(previousStep),
                newStep
        );
        
        return paymentRepository.saveAndPublishEvents(payment, Collections.singletonList(event));
    }

    @Transactional(readOnly = true)
    public Mono<Payment> getPayment(PaymentId paymentId) {
        return paymentRepository.findById(paymentId)
                .switchIfEmpty(Mono.error(new PaymentNotFoundException("Payment not found with ID: " + paymentId)));
    }

    @Transactional(readOnly = true)
    public Mono<Payment> getPaymentByTransactionId(TransactionId transactionId) {
        return paymentRepository.findByTransactionId(transactionId)
                .switchIfEmpty(Mono.error(new PaymentNotFoundException("Payment not found with transaction ID: " + transactionId)));
    }

    @Transactional(readOnly = true)
    public Flux<Payment> getPaymentsByAccount(AccountId accountId) {
        return paymentRepository.findByAccountId(accountId);
    }

    @Transactional(readOnly = true)
    public Flux<Payment> getPaymentsByStatus(PaymentStatus status) {
        return paymentRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public Flux<Payment> getPaymentsByStep(PaymentStep step) {
        return paymentRepository.findByCurrentStep(step);
    }

    public Flux<Payment> retryFailedPayments() {
        return paymentRepository.findByStatus(PaymentStatus.PENDING)
                .filter(payment -> payment.getCurrentStep() != PaymentStep.INITIATED)
                .flatMap(this::processPaymentSaga);
    }
}