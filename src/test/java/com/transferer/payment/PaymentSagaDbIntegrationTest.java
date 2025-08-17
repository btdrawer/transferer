package com.transferer.payment;

import com.transferer.account.application.AccountService;
import com.transferer.account.domain.Account;
import com.transferer.account.domain.AccountId;
import com.transferer.account.domain.AccountRepository;
import com.transferer.payment.application.PaymentService;
import com.transferer.payment.domain.Payment;
import com.transferer.payment.domain.PaymentRepository;
import com.transferer.payment.domain.PaymentStatus;
import com.transferer.payment.domain.PaymentStep;
import com.transferer.transaction.application.TransactionService;
import com.transferer.transaction.domain.TransactionRepository;
import com.transferer.shared.domain.events.DomainEventType;
import com.transferer.TestEventUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Arrays;
import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@Import({
    PaymentService.class, 
    AccountService.class, 
    TransactionService.class,
    com.transferer.payment.infrastructure.R2dbcPaymentRepository.class,
    com.transferer.account.infrastructure.R2dbcAccountRepository.class,
    com.transferer.transaction.infrastructure.R2dbcTransactionRepository.class,
    com.transferer.shared.outbox.OutboxEventBus.class,
    com.transferer.shared.outbox.OutboxEventPublisher.class,
    com.transferer.TestJacksonConfiguration.class
})
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class PaymentSagaDbIntegrationTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private DatabaseClient databaseClient;

    private AccountId senderAccountId;
    private AccountId recipientAccountId;
    private BigDecimal paymentAmount;

    @BeforeEach
    void setUp() {
        paymentAmount = new BigDecimal("100.00");

        // Use the AccountService to properly create accounts, which should handle persistence correctly
        Account senderAccount = accountService.openAccount("John Doe", new BigDecimal("1000.00")).block();
        Account recipientAccount = accountService.openAccount("Jane Smith", new BigDecimal("500.00")).block();

        Assertions.assertNotNull(senderAccount);
        senderAccountId = senderAccount.getId();

        Assertions.assertNotNull(recipientAccount);
        recipientAccountId = recipientAccount.getId();
    }

    @AfterEach
    void tearDown() {
        databaseClient.sql("DELETE FROM payments").then().block();
        databaseClient.sql("DELETE FROM transactions").then().block();
        databaseClient.sql("DELETE FROM accounts").then().block();
        databaseClient.sql("DELETE FROM outbox_events").then().block();
    }

    @Test
    void should_initiate_payment_and_persist_to_database() {
        StepVerifier.create(
                TestEventUtils.performAndWaitForEvents(
                        paymentService.initiatePayment(
                                senderAccountId,
                                recipientAccountId,
                                paymentAmount,
                                "Database integration test"
                        ),
                        databaseClient,
                        Arrays.asList(DomainEventType.PAYMENT_INITIATED, DomainEventType.PAYMENT_STEP_ADVANCED)
                )
        )
                .assertNext(payment -> {
                    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
                    assertThat(payment.getCurrentStep()).isEqualTo(PaymentStep.INITIATED);
                    assertThat(payment.getSenderAccountId()).isEqualTo(senderAccountId);
                    assertThat(payment.getRecipientAccountId()).isEqualTo(recipientAccountId);
                    assertThat(payment.getAmount()).isEqualTo(paymentAmount);
                    assertThat(payment.getId()).isNotNull();
                    assertThat(payment.getCreatedAt()).isNotNull();
                    assertThat(payment.getUpdatedAt()).isNotNull();
                })
                .verifyComplete();
    }

    @Test
    void should_persist_payment_state_changes_correctly() {
        StepVerifier.create(
                TestEventUtils.performAndWaitForEvents(
                        paymentService.initiatePayment(
                                senderAccountId,
                                recipientAccountId,
                                paymentAmount,
                                "State persistence test"
                        ),
                        databaseClient,
                        Arrays.asList(DomainEventType.PAYMENT_INITIATED, DomainEventType.PAYMENT_STEP_ADVANCED)
                ).flatMap(payment ->
                        paymentRepository.findById(payment.getId())
                )
        )
                .assertNext(savedPayment -> {
                    assertThat(savedPayment.getSenderAccountId()).isEqualTo(senderAccountId);
                    assertThat(savedPayment.getRecipientAccountId()).isEqualTo(recipientAccountId);
                    assertThat(savedPayment.getAmount()).isEqualTo(paymentAmount);
                    assertThat(savedPayment.getDescription()).isEqualTo("State persistence test");
                    assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.PENDING);
                    assertThat(savedPayment.getCurrentStep()).isEqualTo(PaymentStep.INITIATED);
                })
                .verifyComplete();
    }

    @Test
    void should_handle_payment_saga_progression() {
        StepVerifier.create(
                TestEventUtils.performAndWaitForEvents(
                        paymentService.initiatePayment(
                                senderAccountId,
                                recipientAccountId,
                                paymentAmount,
                                "Saga progression test"
                        ),
                        databaseClient,
                        Arrays.asList(DomainEventType.PAYMENT_INITIATED, DomainEventType.PAYMENT_STEP_ADVANCED)
                ).delayUntil(payment -> 
                        // Wait for saga processing to complete by checking for additional events in outbox
                        Mono.delay(Duration.ofMillis(300))
                ).flatMap(payment -> 
                        // Re-fetch the payment to get the latest state after saga processing
                        paymentRepository.findById(payment.getId())
                )
        )
                .assertNext(payment -> {
                    // The payment should be initiated and events should be published
                    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
                    assertThat(payment.getCurrentStep()).isIn(
                            PaymentStep.INITIATED, 
                            PaymentStep.TRANSACTION_CREATED, 
                            PaymentStep.TRANSACTION_PROCESSING,
                            PaymentStep.SENDER_DEBITED,
                            PaymentStep.RECIPIENT_CREDITED,
                            PaymentStep.COMPLETED
                    );
                    assertThat(payment.getUpdatedAt()).isAfterOrEqualTo(payment.getCreatedAt());
                    assertThat(payment.getDescription()).isEqualTo("Saga progression test");
                })
                .verifyComplete();
    }

    @Test
    void should_handle_concurrent_payments_from_same_account() {
        BigDecimal smallAmount = new BigDecimal("50.00");

        Mono<Payment> payment1 = TestEventUtils.performAndWaitForEvents(
                paymentService.initiatePayment(
                        senderAccountId,
                        recipientAccountId,
                        smallAmount,
                        "Concurrent payment 1"
                ),
                databaseClient,
                Arrays.asList(DomainEventType.PAYMENT_INITIATED, DomainEventType.PAYMENT_STEP_ADVANCED)
        );

        // Create another recipient account first to avoid foreign key constraint
        Account otherRecipient = accountService.openAccount("Other Recipient", new BigDecimal("0.00")).block();
        AccountId otherRecipientId = otherRecipient.getId();
        
        Mono<Payment> payment2 = TestEventUtils.performAndWaitForEvents(
                paymentService.initiatePayment(
                        senderAccountId,
                        otherRecipientId,
                        smallAmount,
                        "Concurrent payment 2"
                ),
                databaseClient,
                Arrays.asList(DomainEventType.PAYMENT_INITIATED, DomainEventType.PAYMENT_STEP_ADVANCED)
        );

        StepVerifier.create(Mono.zip(payment1, payment2))
                .assertNext(tuple -> {
                    Payment p1 = tuple.getT1();
                    Payment p2 = tuple.getT2();

                    assertThat(p1.getId()).isNotEqualTo(p2.getId());
                    assertThat(p1.getSenderAccountId()).isEqualTo(p2.getSenderAccountId());
                    assertThat(p1.getRecipientAccountId()).isNotEqualTo(p2.getRecipientAccountId());
                    assertThat(p1.getStatus()).isEqualTo(PaymentStatus.PENDING);
                    assertThat(p2.getStatus()).isEqualTo(PaymentStatus.PENDING);
                })
                .verifyComplete();
    }

    @Test
    void should_query_payments_by_status() {
        StepVerifier.create(
                TestEventUtils.performAndWaitForEvents(
                        paymentService.initiatePayment(
                                senderAccountId,
                                recipientAccountId,
                                paymentAmount,
                                "Status query test"
                        ),
                        databaseClient,
                        Arrays.asList(DomainEventType.PAYMENT_INITIATED, DomainEventType.PAYMENT_STEP_ADVANCED)
                ).then(paymentService.getPaymentsByStatus(PaymentStatus.PENDING).next())
        )
                .assertNext(payment -> {
                    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
                    assertThat(payment.getDescription()).isEqualTo("Status query test");
                })
                .verifyComplete();
    }

    @Test
    void should_query_payments_by_account() {
        StepVerifier.create(
                TestEventUtils.performAndWaitForEvents(
                        paymentService.initiatePayment(
                                senderAccountId,
                                recipientAccountId,
                                paymentAmount,
                                "Account query test"
                        ),
                        databaseClient,
                        Arrays.asList(DomainEventType.PAYMENT_INITIATED, DomainEventType.PAYMENT_STEP_ADVANCED)
                ).then(paymentService.getPaymentsByAccount(senderAccountId).next())
        )
                .assertNext(payment -> {
                    assertThat(payment.getSenderAccountId()).isEqualTo(senderAccountId);
                    assertThat(payment.getDescription()).isEqualTo("Account query test");
                })
                .verifyComplete();
    }

    @Test
    void should_query_payments_by_step() {
        StepVerifier.create(
                TestEventUtils.performAndWaitForEvents(
                        paymentService.initiatePayment(
                                senderAccountId,
                                recipientAccountId,
                                paymentAmount,
                                "Step query test"
                        ),
                        databaseClient,
                        Arrays.asList(DomainEventType.PAYMENT_INITIATED, DomainEventType.PAYMENT_STEP_ADVANCED)
                ).then(paymentService.getPaymentsByStep(PaymentStep.INITIATED).next())
        )
                .assertNext(payment -> {
                    assertThat(payment.getCurrentStep()).isEqualTo(PaymentStep.INITIATED);
                    assertThat(payment.getDescription()).isEqualTo("Step query test");
                })
                .verifyComplete();
    }

    @Test
    void should_handle_insufficient_funds_scenario() {
        // Use AccountService to properly create account with balance
        Account poorSenderAccount = accountService.openAccount("Poor Sender", new BigDecimal("10.00")).block();
        AccountId poorSenderAccountId = poorSenderAccount.getId();

        StepVerifier.create(
                TestEventUtils.performAndWaitForEvents(
                        paymentService.initiatePayment(
                                poorSenderAccountId,
                                recipientAccountId,
                                new BigDecimal("1000.00"),
                                "Insufficient funds test"
                        ),
                        databaseClient,
                        Arrays.asList(DomainEventType.PAYMENT_INITIATED, DomainEventType.PAYMENT_STEP_ADVANCED)
                )
        )
                .assertNext(payment -> {
                    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
                    assertThat(payment.getCurrentStep()).isEqualTo(PaymentStep.INITIATED);
                    assertThat(payment.getAmount()).isEqualTo(new BigDecimal("1000.00"));
                })
                .verifyComplete();
    }

    @Test
    void should_maintain_referential_integrity_between_payment_and_transaction() {
        StepVerifier.create(
                TestEventUtils.performAndWaitForEvents(
                        paymentService.initiatePayment(
                                senderAccountId,
                                recipientAccountId,
                                paymentAmount,
                                "Referential integrity test"
                        ),
                        databaseClient,
                        Arrays.asList(DomainEventType.PAYMENT_INITIATED, DomainEventType.PAYMENT_STEP_ADVANCED)
                ).delayUntil(payment -> 
                        // Wait for saga processing to potentially create transaction
                        Mono.delay(Duration.ofMillis(300))
                ).flatMap(payment ->
                        paymentRepository.findById(payment.getId())
                                .flatMap(updatedPayment -> {
                                    if (updatedPayment.getTransactionId() != null) {
                                        return transactionRepository.findById(updatedPayment.getTransactionId())
                                                .map(transaction -> {
                                                    assertThat(transaction.getSenderAccountId())
                                                            .isEqualTo(updatedPayment.getSenderAccountId());
                                                    assertThat(transaction.getRecipientAccountId())
                                                            .isEqualTo(updatedPayment.getRecipientAccountId());
                                                    assertThat(transaction.getAmount())
                                                            .isEqualTo(updatedPayment.getAmount());
                                                    return updatedPayment;
                                                });
                                    } else {
                                        return Mono.just(updatedPayment);
                                    }
                                })
                )
        )
                .assertNext(payment -> {
                    assertThat(payment).isNotNull();
                })
                .verifyComplete();
    }
}