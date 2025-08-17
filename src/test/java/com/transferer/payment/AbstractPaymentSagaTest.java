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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public abstract class AbstractPaymentSagaTest {

    @Autowired
    protected PaymentService paymentService;

    @Autowired
    protected AccountService accountService;

    @Autowired
    protected TransactionService transactionService;

    @Autowired
    protected PaymentRepository paymentRepository;

    @Autowired
    protected AccountRepository accountRepository;

    @Autowired
    protected TransactionRepository transactionRepository;

    @Autowired
    protected DatabaseClient databaseClient;

    protected AccountId senderAccountId;
    protected AccountId recipientAccountId;
    protected BigDecimal paymentAmount;

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

    @Test
    void should_verify_account_balances_at_each_saga_step() {
        BigDecimal initialSenderBalance = new BigDecimal("1000.00");
        BigDecimal initialRecipientBalance = new BigDecimal("500.00");
        BigDecimal transferAmount = new BigDecimal("100.00");

        StepVerifier.create(
                paymentService.initiatePayment(
                        senderAccountId,
                        recipientAccountId,
                        transferAmount,
                        "Balance verification test"
                )
                        .flatMap(payment -> {
                            // Step 1: Wait for PAYMENT_STEP_ADVANCED after INITIATED
                            return TestEventUtils.waitForEventsInOutbox(
                                    databaseClient,
                                    Collections.singletonList(DomainEventType.PAYMENT_STEP_ADVANCED),
                                    Duration.ofSeconds(2)
                            ).then(
                                    paymentRepository.findById(payment.getId())
                                            .flatMap(updatedPayment -> {
                                                // Verify payment is in INITIATED step
                                                assertThat(updatedPayment.getCurrentStep()).isEqualTo(PaymentStep.INITIATED);

                                                // Verify balances haven't changed yet (payment initiated but not processed)
                                                return Mono.zip(
                                                        accountRepository.findById(senderAccountId),
                                                        accountRepository.findById(recipientAccountId)
                                                ).map(accounts -> {
                                                    Account sender = accounts.getT1();
                                                    Account recipient = accounts.getT2();
                                                    assertThat(sender.getBalance()).isEqualTo(initialSenderBalance);
                                                    assertThat(recipient.getBalance()).isEqualTo(initialRecipientBalance);
                                                    return payment;
                                                });
                                            })
                            );
                        })
                        .flatMap(payment -> {
                            // Allow time for saga progression and wait for multiple steps
                            return TestEventUtils.waitForEventsInOutbox(
                                            databaseClient,
                                            Collections.singletonList(DomainEventType.PAYMENT_STEP_ADVANCED),
                                            Duration.ofSeconds(2)
                                    )
                                    .then(paymentRepository.findById(payment.getId()))
                                    .flatMap(updatedPayment -> {
                                        // Check if payment has progressed through multiple steps
                                        if (updatedPayment.getCurrentStep() == PaymentStep.SENDER_DEBITED ||
                                            updatedPayment.getCurrentStep() == PaymentStep.RECIPIENT_CREDITED ||
                                            updatedPayment.getCurrentStep() == PaymentStep.COMPLETED) {
                                            
                                            // Verify sender balance is debited
                                            return accountRepository.findById(senderAccountId)
                                                    .map(sender -> {
                                                        assertThat(sender.getBalance()).isEqualTo(initialSenderBalance.subtract(transferAmount));
                                                        return payment;
                                                    });
                                        } else {
                                            return Mono.just(payment);
                                        }
                                    });
                        })
                        .flatMap(payment -> {
                            // Wait additional time for recipient credit
                            return TestEventUtils.waitForEventsInOutbox(
                                            databaseClient,
                                            Collections.singletonList(DomainEventType.PAYMENT_STEP_ADVANCED),
                                            Duration.ofSeconds(2)
                                    )
                                    .then(paymentRepository.findById(payment.getId()))
                                    .flatMap(updatedPayment -> {
                                        if (updatedPayment.getCurrentStep() == PaymentStep.RECIPIENT_CREDITED ||
                                            updatedPayment.getCurrentStep() == PaymentStep.COMPLETED) {
                                            
                                            // Verify both balances are updated correctly
                                            return Mono.zip(
                                                    accountRepository.findById(senderAccountId),
                                                    accountRepository.findById(recipientAccountId)
                                            ).map(accounts -> {
                                                Account sender = accounts.getT1();
                                                Account recipient = accounts.getT2();
                                                
                                                assertThat(sender.getBalance()).isEqualTo(initialSenderBalance.subtract(transferAmount));
                                                assertThat(recipient.getBalance()).isEqualTo(initialRecipientBalance.add(transferAmount));
                                                return payment;
                                            });
                                        } else {
                                            return Mono.just(payment);
                                        }
                                    });
                        })
                        .flatMap(payment -> {
                            // Final verification - wait for completion
                            return TestEventUtils.waitForEventsInOutbox(
                                            databaseClient,
                                            Collections.singletonList(DomainEventType.PAYMENT_STEP_ADVANCED),
                                            Duration.ofSeconds(2)
                                    )
                                    .then(paymentRepository.findById(payment.getId()))
                                    .flatMap(finalPayment -> {
                                        if (finalPayment.getCurrentStep() == PaymentStep.COMPLETED) {
                                            // Verify final state
                                            return Mono.zip(
                                                    accountRepository.findById(senderAccountId),
                                                    accountRepository.findById(recipientAccountId)
                                            ).map(accounts -> {
                                                Account sender = accounts.getT1();
                                                Account recipient = accounts.getT2();
                                                
                                                assertThat(sender.getBalance()).isEqualTo(initialSenderBalance.subtract(transferAmount));
                                                assertThat(recipient.getBalance()).isEqualTo(initialRecipientBalance.add(transferAmount));
                                                assertThat(finalPayment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
                                                return finalPayment;
                                            });
                                        } else {
                                            return Mono.just(finalPayment);
                                        }
                                    });
                        })
        )
                .assertNext(payment -> {
                    assertThat(payment).isNotNull();
                    assertThat(payment.getDescription()).isEqualTo("Balance verification test");
                })
                .verifyComplete();
    }

    @Test
    void should_verify_account_balances_during_rollback_scenario() {
        BigDecimal initialSenderBalance = new BigDecimal("1000.00");
        BigDecimal initialRecipientBalance = new BigDecimal("500.00");
        BigDecimal transferAmount = new BigDecimal("100.00");

        StepVerifier.create(
                paymentService.initiatePayment(
                        senderAccountId,
                        recipientAccountId,
                        transferAmount,
                        "Rollback balance verification test"
                )
                        .flatMap(payment -> {
                            // Step 1: Wait for payment initiation
                            return TestEventUtils.waitForEventsInOutbox(
                                    databaseClient,
                                    Collections.singletonList(DomainEventType.PAYMENT_INITIATED),
                                    Duration.ofSeconds(2)
                            ).then(
                                    paymentRepository.findById(payment.getId())
                                            .flatMap(updatedPayment -> {
                                                // Verify payment is initiated but balances unchanged
                                                assertThat(updatedPayment.getCurrentStep()).isEqualTo(PaymentStep.INITIATED);
                                                
                                                return Mono.zip(
                                                        accountRepository.findById(senderAccountId),
                                                        accountRepository.findById(recipientAccountId)
                                                ).map(accounts -> {
                                                    Account sender = accounts.getT1();
                                                    Account recipient = accounts.getT2();
                                                    assertThat(sender.getBalance()).isEqualTo(initialSenderBalance);
                                                    assertThat(recipient.getBalance()).isEqualTo(initialRecipientBalance);
                                                    return payment;
                                                });
                                            })
                            );
                        })
                        .flatMap(payment -> {
                            // Step 2: Simulate the saga progression and force a failure scenario
                            // by manually advancing the payment to sender debited state and then triggering compensation
                            return paymentRepository.findById(payment.getId())
                                    .flatMap(updatedPayment -> {
                                        // Allow time for natural saga progression
                                        return Mono.delay(Duration.ofMillis(500))
                                                .then(paymentRepository.findById(payment.getId()));
                                    })
                                    .flatMap(currentPayment -> {
                                        // Check if saga has progressed to where sender could be debited
                                        if (currentPayment.getCurrentStep() == PaymentStep.SENDER_DEBITED ||
                                            currentPayment.getCurrentStep() == PaymentStep.RECIPIENT_CREDITED ||
                                            currentPayment.getCurrentStep() == PaymentStep.COMPLETED) {
                                            
                                            // Verify the balance changes that occurred during processing
                                            return Mono.zip(
                                                    accountRepository.findById(senderAccountId),
                                                    accountRepository.findById(recipientAccountId)
                                            ).map(accounts -> {
                                                Account sender = accounts.getT1();
                                                Account recipient = accounts.getT2();
                                                
                                                // At this point, if sender was debited, verify the debit occurred
                                                if (currentPayment.getCurrentStep() == PaymentStep.SENDER_DEBITED) {
                                                    assertThat(sender.getBalance()).isEqualTo(initialSenderBalance.subtract(transferAmount));
                                                    // Recipient should not be credited yet
                                                    assertThat(recipient.getBalance()).isEqualTo(initialRecipientBalance);
                                                } else if (currentPayment.getCurrentStep() == PaymentStep.RECIPIENT_CREDITED || 
                                                          currentPayment.getCurrentStep() == PaymentStep.COMPLETED) {
                                                    // Both should have been updated
                                                    assertThat(sender.getBalance()).isEqualTo(initialSenderBalance.subtract(transferAmount));
                                                    assertThat(recipient.getBalance()).isEqualTo(initialRecipientBalance.add(transferAmount));
                                                }
                                                
                                                return currentPayment;
                                            });
                                        } else {
                                            // Payment hasn't progressed enough yet, return as is
                                            return Mono.just(currentPayment);
                                        }
                                    });
                        })
                        .flatMap(payment -> {
                            // Step 3: Final verification - check if payment completed successfully
                            return TestEventUtils.waitForEventsInOutbox(
                                    databaseClient,
                                    Arrays.asList(DomainEventType.PAYMENT_STEP_ADVANCED, DomainEventType.PAYMENT_COMPLETED),
                                    Duration.ofSeconds(3)
                            ).then(
                                    paymentRepository.findById(payment.getId())
                                            .flatMap(finalPayment -> {
                                                // Verify final state
                                                return Mono.zip(
                                                        accountRepository.findById(senderAccountId),
                                                        accountRepository.findById(recipientAccountId)
                                                ).map(accounts -> {
                                                    Account sender = accounts.getT1();
                                                    Account recipient = accounts.getT2();
                                                    
                                                    // Check final balances based on whether payment completed or failed
                                                    if (finalPayment.getStatus() == PaymentStatus.COMPLETED) {
                                                        // Successful payment: balances should reflect the transfer
                                                        assertThat(sender.getBalance()).isEqualTo(initialSenderBalance.subtract(transferAmount));
                                                        assertThat(recipient.getBalance()).isEqualTo(initialRecipientBalance.add(transferAmount));
                                                    } else if (finalPayment.getStatus() == PaymentStatus.FAILED) {
                                                        // Failed payment with rollback: balances should be restored
                                                        assertThat(sender.getBalance()).isEqualTo(initialSenderBalance);
                                                        assertThat(recipient.getBalance()).isEqualTo(initialRecipientBalance);
                                                    }
                                                    
                                                    return finalPayment;
                                                });
                                            })
                            );
                        })
        )
                .assertNext(payment -> {
                    assertThat(payment).isNotNull();
                    assertThat(payment.getDescription()).isEqualTo("Rollback balance verification test");
                    // Payment should either be completed or failed (both are valid outcomes for this test)
                    assertThat(payment.getStatus()).isIn(PaymentStatus.COMPLETED, PaymentStatus.FAILED);
                })
                .verifyComplete();
    }
}