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

class PaymentRollbackTest {
    
    private AccountId senderAccountId;
    private AccountId recipientAccountId;
    private BigDecimal amount;

    @BeforeEach
    void setUp() {
        senderAccountId = AccountId.of("sender-123");
        recipientAccountId = AccountId.of("recipient-456");
        amount = new BigDecimal("100.00");
    }

    @Test
    void should_identify_when_compensation_is_required() {
        Payment payment = new Payment(senderAccountId, recipientAccountId, amount, "Compensation test");
        payment.startProcessing();
        
        assertThat(payment.requiresCompensation()).isFalse();
        
        payment.advanceToStep(PaymentStep.TRANSACTION_PROCESSING);
        assertThat(payment.requiresCompensation()).isFalse();
        
        payment.advanceToStep(PaymentStep.SENDER_DEBITED);
        assertThat(payment.requiresCompensation()).isTrue();
        
        payment.advanceToStep(PaymentStep.RECIPIENT_CREDITED);
        assertThat(payment.requiresCompensation()).isTrue();
    }

    @Test
    void should_handle_compensation_flow_correctly() {
        Payment payment = new Payment(senderAccountId, recipientAccountId, amount, "Rollback flow test");
        TransactionId transactionId = TransactionId.generate();
        payment.setTransactionId(transactionId);
        payment.startProcessing();
        payment.advanceToStep(PaymentStep.SENDER_DEBITED);
        
        assertThat(payment.requiresCompensation()).isTrue();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PROCESSING);
        
        payment.startCompensation();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPENSATING);
        assertThat(payment.getCurrentStep()).isEqualTo(PaymentStep.COMPENSATING_SENDER_CREDIT);
        
        payment.markAsCompensated();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getCurrentStep()).isEqualTo(PaymentStep.COMPENSATED);
    }

    @Test
    void should_prevent_compensation_on_non_processing_payments() {
        Payment payment = new Payment(senderAccountId, recipientAccountId, amount, "Non-processing test");
        
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        
        IllegalStateException exception = org.junit.jupiter.api.Assertions.assertThrows(
            IllegalStateException.class,
            () -> payment.startCompensation()
        );
        assertThat(exception.getMessage()).contains("Can only start compensation for processing payments");
    }

    @Test
    void should_prevent_compensation_if_sender_not_debited() {
        Payment payment = new Payment(senderAccountId, recipientAccountId, amount, "No debit test");
        payment.startProcessing();
        payment.advanceToStep(PaymentStep.TRANSACTION_PROCESSING);
        
        assertThat(payment.requiresCompensation()).isFalse();
        
        payment.markAsFailed("Transaction failed");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getCurrentStep()).isEqualTo(PaymentStep.FAILED);
    }

    @Test
    void should_track_rollback_scenarios_after_successful_debit() {
        Payment payment = new Payment(senderAccountId, recipientAccountId, amount, "Post-debit rollback");
        payment.startProcessing();
        payment.advanceToStep(PaymentStep.SENDER_DEBITED);
        
        assertThat(payment.requiresCompensation()).isTrue();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PROCESSING);
        assertThat(payment.getCurrentStep()).isEqualTo(PaymentStep.SENDER_DEBITED);
        
        payment.startCompensation();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPENSATING);
        assertThat(payment.getCurrentStep()).isEqualTo(PaymentStep.COMPENSATING_SENDER_CREDIT);
    }

    @Test
    void should_track_rollback_scenarios_after_recipient_credit() {
        Payment payment = new Payment(senderAccountId, recipientAccountId, amount, "Post-credit rollback");
        payment.startProcessing();
        payment.advanceToStep(PaymentStep.SENDER_DEBITED);
        payment.advanceToStep(PaymentStep.RECIPIENT_CREDITED);
        
        assertThat(payment.requiresCompensation()).isTrue();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PROCESSING);
        assertThat(payment.getCurrentStep()).isEqualTo(PaymentStep.RECIPIENT_CREDITED);
        
        payment.startCompensation();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPENSATING);
        assertThat(payment.getCurrentStep()).isEqualTo(PaymentStep.COMPENSATING_SENDER_CREDIT);
    }

    @Test
    void should_prevent_double_compensation_attempt() {
        Payment payment = new Payment(senderAccountId, recipientAccountId, amount, "Double compensation test");
        payment.startProcessing();
        payment.advanceToStep(PaymentStep.SENDER_DEBITED);
        payment.startCompensation();
        
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPENSATING);
        
        IllegalStateException exception = org.junit.jupiter.api.Assertions.assertThrows(
            IllegalStateException.class,
            () -> payment.startCompensation()
        );
        assertThat(exception.getMessage()).contains("Can only start compensation for processing payments");
    }

    @Test
    void should_prevent_compensation_marking_on_non_compensating_payment() {
        Payment payment = new Payment(senderAccountId, recipientAccountId, amount, "Invalid compensation marking");
        payment.startProcessing();
        
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PROCESSING);
        
        IllegalStateException exception = org.junit.jupiter.api.Assertions.assertThrows(
            IllegalStateException.class,
            () -> payment.markAsCompensated()
        );
        assertThat(exception.getMessage()).contains("Can only mark compensating payments as compensated");
    }

    @Test
    void should_handle_failed_compensation_scenario() {
        Payment payment = new Payment(senderAccountId, recipientAccountId, amount, "Failed compensation");
        payment.startProcessing();
        payment.advanceToStep(PaymentStep.SENDER_DEBITED);
        payment.startCompensation();
        
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPENSATING);
        assertThat(payment.getCurrentStep()).isEqualTo(PaymentStep.COMPENSATING_SENDER_CREDIT);
        
        payment.markAsFailed("Compensation failed - account closed");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getCurrentStep()).isEqualTo(PaymentStep.FAILED);
        assertThat(payment.getFailureReason()).contains("Compensation failed - account closed");
    }

    @Test
    void should_maintain_compensation_state_consistency() {
        Payment payment = new Payment(senderAccountId, recipientAccountId, amount, "State consistency test");
        payment.startProcessing();
        payment.advanceToStep(PaymentStep.SENDER_DEBITED);
        
        PaymentStatus statusBeforeCompensation = payment.getStatus();
        PaymentStep stepBeforeCompensation = payment.getCurrentStep();
        
        payment.startCompensation();
        
        assertThat(payment.getStatus()).isNotEqualTo(statusBeforeCompensation);
        assertThat(payment.getCurrentStep()).isNotEqualTo(stepBeforeCompensation);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPENSATING);
        assertThat(payment.getCurrentStep()).isEqualTo(PaymentStep.COMPENSATING_SENDER_CREDIT);
    }

    @Test
    void should_prevent_completion_after_compensation_started() {
        Payment payment = new Payment(senderAccountId, recipientAccountId, amount, "Completion after compensation");
        payment.startProcessing();
        payment.advanceToStep(PaymentStep.SENDER_DEBITED);
        payment.startCompensation();
        
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPENSATING);
        
        IllegalStateException exception = org.junit.jupiter.api.Assertions.assertThrows(
            IllegalStateException.class,
            () -> payment.markAsCompleted()
        );
        assertThat(exception.getMessage()).contains("Cannot mark compensating payment as completed");
    }

    @Test
    void should_track_timestamp_updates_during_compensation() {
        Payment payment = new Payment(senderAccountId, recipientAccountId, amount, "Timestamp tracking");
        payment.startProcessing();
        payment.advanceToStep(PaymentStep.SENDER_DEBITED);
        
        assertThat(payment.getUpdatedAt()).isNotNull();
        java.time.LocalDateTime beforeCompensation = payment.getUpdatedAt();
        
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        payment.startCompensation();
        assertThat(payment.getUpdatedAt()).isAfter(beforeCompensation);
        
        java.time.LocalDateTime beforeCompensated = payment.getUpdatedAt();
        
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        payment.markAsCompensated();
        assertThat(payment.getUpdatedAt()).isAfter(beforeCompensated);
    }

    @Test
    void should_preserve_transaction_id_during_rollback() {
        Payment payment = new Payment(senderAccountId, recipientAccountId, amount, "Transaction ID preservation");
        TransactionId transactionId = TransactionId.generate();
        
        payment.setTransactionId(transactionId);
        payment.startProcessing();
        payment.advanceToStep(PaymentStep.SENDER_DEBITED);
        
        assertThat(payment.getTransactionId()).isEqualTo(transactionId);
        
        payment.startCompensation();
        assertThat(payment.getTransactionId()).isEqualTo(transactionId);
        
        payment.markAsCompensated();
        assertThat(payment.getTransactionId()).isEqualTo(transactionId);
    }
}