package com.transferer.payment;

import com.transferer.account.domain.AccountId;
import com.transferer.payment.domain.Payment;
import com.transferer.payment.domain.PaymentStatus;
import com.transferer.payment.domain.PaymentStep;
import com.transferer.transaction.domain.TransactionId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentSagaIntegrationTest {
    
    private AccountId senderAccountId;
    private AccountId recipientAccountId;
    private BigDecimal amount;
    private String description;

    @BeforeEach
    void setUp() {
        senderAccountId = AccountId.of("sender-123");
        recipientAccountId = AccountId.of("recipient-456");
        amount = new BigDecimal("100.00");
        description = "Test payment";
    }

    @Test
    void should_create_payment_with_correct_initial_state() {
        Payment payment = new Payment(senderAccountId, recipientAccountId, amount, description);
        
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getCurrentStep()).isEqualTo(PaymentStep.INITIATED);
        assertThat(payment.getSenderAccountId()).isEqualTo(senderAccountId);
        assertThat(payment.getRecipientAccountId()).isEqualTo(recipientAccountId);
        assertThat(payment.getAmount()).isEqualTo(amount);
        assertThat(payment.getDescription()).isEqualTo(description);
        assertThat(payment.getId()).isNotNull();
        assertThat(payment.getCreatedAt()).isNotNull();
    }

    @Test
    void should_track_payment_state_progression_through_successful_flow() {
        Payment payment = new Payment(senderAccountId, recipientAccountId, amount, description);
        TransactionId transactionId = TransactionId.generate();
        
        assertThat(payment.getCurrentStep()).isEqualTo(PaymentStep.INITIATED);
        
        payment.setTransactionId(transactionId);
        assertThat(payment.getCurrentStep()).isEqualTo(PaymentStep.TRANSACTION_CREATED);
        assertThat(payment.getTransactionId()).isEqualTo(transactionId);
        
        payment.advanceToStep(PaymentStep.TRANSACTION_PROCESSING);
        assertThat(payment.getCurrentStep()).isEqualTo(PaymentStep.TRANSACTION_PROCESSING);
        
        payment.advanceToStep(PaymentStep.SENDER_DEBITED);
        assertThat(payment.getCurrentStep()).isEqualTo(PaymentStep.SENDER_DEBITED);
        
        payment.advanceToStep(PaymentStep.RECIPIENT_CREDITED);
        assertThat(payment.getCurrentStep()).isEqualTo(PaymentStep.RECIPIENT_CREDITED);
        
        payment.markAsCompleted();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(payment.getCurrentStep()).isEqualTo(PaymentStep.COMPLETED);
        assertThat(payment.getCompletedAt()).isNotNull();
    }

    @Test
    void should_handle_payment_failure_correctly() {
        Payment payment = new Payment(senderAccountId, recipientAccountId, amount, description);
        String failureReason = "Insufficient funds";
        
        payment.markAsFailed(failureReason);
        
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getCurrentStep()).isEqualTo(PaymentStep.FAILED);
        assertThat(payment.getFailureReason()).isEqualTo(failureReason);
    }

    @Test
    void should_handle_compensation_state_correctly() {
        Payment payment = new Payment(senderAccountId, recipientAccountId, amount, description);
        payment.startProcessing();
        payment.advanceToStep(PaymentStep.SENDER_DEBITED);
        
        assertThat(payment.requiresCompensation()).isTrue();
        
        payment.startCompensation();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPENSATING);
        assertThat(payment.getCurrentStep()).isEqualTo(PaymentStep.COMPENSATING_SENDER_CREDIT);
        
        payment.markAsCompensated();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getCurrentStep()).isEqualTo(PaymentStep.COMPENSATED);
    }

    @Test
    void should_determine_compensation_requirement_correctly() {
        Payment payment = new Payment(senderAccountId, recipientAccountId, amount, description);
        
        assertThat(payment.requiresCompensation()).isFalse();
        
        payment.startProcessing();
        payment.advanceToStep(PaymentStep.TRANSACTION_PROCESSING);
        assertThat(payment.requiresCompensation()).isFalse();
        
        payment.advanceToStep(PaymentStep.SENDER_DEBITED);
        assertThat(payment.requiresCompensation()).isTrue();
        
        payment.advanceToStep(PaymentStep.RECIPIENT_CREDITED);
        assertThat(payment.requiresCompensation()).isTrue();
    }

    @Test
    void should_prevent_duplicate_transaction_id_assignment() {
        Payment payment = new Payment(senderAccountId, recipientAccountId, amount, description);
        TransactionId transactionId = TransactionId.generate();
        
        payment.setTransactionId(transactionId);
        assertThat(payment.getTransactionId()).isEqualTo(transactionId);
        
        IllegalStateException exception = org.junit.jupiter.api.Assertions.assertThrows(
            IllegalStateException.class,
            () -> payment.setTransactionId(TransactionId.generate())
        );
        assertThat(exception.getMessage()).contains("Transaction ID is already set");
    }

    @Test
    void should_prevent_invalid_state_transitions() {
        Payment payment = new Payment(senderAccountId, recipientAccountId, amount, description);
        payment.markAsFailed("Test failure");
        
        IllegalStateException exception = org.junit.jupiter.api.Assertions.assertThrows(
            IllegalStateException.class,
            () -> payment.markAsCompleted()
        );
        assertThat(exception.getMessage()).contains("Cannot mark failed payment as completed");
    }

    @Test
    void should_validate_payment_parameters() {
        org.junit.jupiter.api.Assertions.assertThrows(
            NullPointerException.class,
            () -> new Payment(null, recipientAccountId, amount, description)
        );

        org.junit.jupiter.api.Assertions.assertThrows(
            NullPointerException.class,
            () -> new Payment(senderAccountId, null, amount, description)
        );

        org.junit.jupiter.api.Assertions.assertThrows(
            NullPointerException.class,
            () -> new Payment(senderAccountId, recipientAccountId, null, description)
        );

        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> new Payment(senderAccountId, recipientAccountId, BigDecimal.ZERO, description)
        );

        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> new Payment(senderAccountId, recipientAccountId, new BigDecimal("-10"), description)
        );
        
        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> new Payment(senderAccountId, senderAccountId, amount, description)
        );
    }
}