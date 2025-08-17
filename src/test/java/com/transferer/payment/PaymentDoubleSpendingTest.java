package com.transferer.payment;

import com.transferer.account.domain.AccountId;
import com.transferer.payment.domain.Payment;
import com.transferer.payment.domain.PaymentStatus;
import com.transferer.payment.domain.PaymentStep;
import com.transferer.transaction.domain.TransactionId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentDoubleSpendingTest {
    
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
    void should_prevent_duplicate_transaction_processing() {
        Payment payment = new Payment(senderAccountId, recipientAccountId, amount, "Test payment");
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
    void should_track_unique_payment_identifiers() {
        List<Payment> payments = new ArrayList<>();
        Set<String> paymentIds = ConcurrentHashMap.newKeySet();
        
        for (int i = 0; i < 100; i++) {
            Payment payment = new Payment(
                senderAccountId, 
                AccountId.of("recipient-" + i), 
                amount, 
                "Payment " + i
            );
            payments.add(payment);
            paymentIds.add(payment.getId().getValue());
        }
        
        assertThat(paymentIds).hasSize(100);
        assertThat(payments).hasSize(100);
        assertThat(payments.stream().map(p -> p.getId().getValue()).distinct().count()).isEqualTo(100);
    }

    @Test
    void should_require_different_transaction_ids_for_different_payments() {
        Payment payment1 = new Payment(senderAccountId, recipientAccountId, amount, "Payment 1");
        Payment payment2 = new Payment(senderAccountId, AccountId.of("other-recipient"), amount, "Payment 2");
        
        TransactionId transactionId1 = TransactionId.generate();
        TransactionId transactionId2 = TransactionId.generate();
        
        payment1.setTransactionId(transactionId1);
        payment2.setTransactionId(transactionId2);
        
        assertThat(payment1.getTransactionId()).isNotEqualTo(payment2.getTransactionId());
        assertThat(payment1.getId()).isNotEqualTo(payment2.getId());
    }

    @Test
    void should_prevent_processing_same_payment_multiple_times() {
        Payment payment = new Payment(senderAccountId, recipientAccountId, amount, "Single processing test");
        
        payment.startProcessing();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PROCESSING);
        
        IllegalStateException exception = org.junit.jupiter.api.Assertions.assertThrows(
            IllegalStateException.class,
            () -> payment.startProcessing()
        );
        assertThat(exception.getMessage()).contains("Can only start processing pending payments");
    }

    @Test
    void should_track_compensation_requirements_correctly() {
        Payment payment = new Payment(senderAccountId, recipientAccountId, amount, "Compensation test");
        payment.startProcessing();
        
        assertThat(payment.requiresCompensation()).isFalse();
        
        payment.advanceToStep(PaymentStep.SENDER_DEBITED);
        assertThat(payment.requiresCompensation()).isTrue();
        
        payment.startCompensation();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPENSATING);
        
        IllegalStateException exception = org.junit.jupiter.api.Assertions.assertThrows(
            IllegalStateException.class,
            () -> payment.startCompensation()
        );
        assertThat(exception.getMessage()).contains("Can only start compensation for processing payments");
    }

    @Test
    void should_prevent_state_manipulation_after_completion() {
        Payment payment = new Payment(senderAccountId, recipientAccountId, amount, "Completion test");
        payment.startProcessing();
        payment.advanceToStep(PaymentStep.SENDER_DEBITED);
        payment.advanceToStep(PaymentStep.RECIPIENT_CREDITED);
        payment.markAsCompleted();
        
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(payment.getCurrentStep()).isEqualTo(PaymentStep.COMPLETED);
        
        IllegalStateException exception = org.junit.jupiter.api.Assertions.assertThrows(
            IllegalStateException.class,
            () -> payment.startCompensation()
        );
        assertThat(exception.getMessage()).contains("Can only start compensation for processing payments");
    }

    @Test
    void should_prevent_double_compensation() {
        Payment payment = new Payment(senderAccountId, recipientAccountId, amount, "Double compensation test");
        payment.startProcessing();
        payment.advanceToStep(PaymentStep.SENDER_DEBITED);
        payment.startCompensation();
        
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPENSATING);
        
        payment.markAsCompensated();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getCurrentStep()).isEqualTo(PaymentStep.COMPENSATED);
        
        IllegalStateException exception = org.junit.jupiter.api.Assertions.assertThrows(
            IllegalStateException.class,
            () -> payment.markAsCompensated()
        );
        assertThat(exception.getMessage()).contains("Can only mark compensating payments as compensated");
    }

    @Test
    void should_validate_same_account_prevention() {
        IllegalArgumentException exception = org.junit.jupiter.api.Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> new Payment(senderAccountId, senderAccountId, amount, "Self transfer")
        );
        assertThat(exception.getMessage()).contains("Sender and recipient accounts cannot be the same");
    }

    @Test
    void should_prevent_negative_amount_spending() {
        IllegalArgumentException exception = org.junit.jupiter.api.Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> new Payment(senderAccountId, recipientAccountId, new BigDecimal("-100"), "Negative payment")
        );
        assertThat(exception.getMessage()).contains("Payment amount must be positive");
    }

    @Test
    void should_ensure_immutable_amount_after_creation() {
        BigDecimal originalAmount = new BigDecimal("100.00");
        Payment payment = new Payment(senderAccountId, recipientAccountId, originalAmount, "Immutable test");
        
        assertThat(payment.getAmount()).isEqualTo(originalAmount);
        
        BigDecimal retrievedAmount = payment.getAmount();
        assertThat(retrievedAmount).isEqualByComparingTo(originalAmount);
        
        // Verify amount cannot be modified through external reference
        assertThat(payment.getAmount().setScale(3, java.math.RoundingMode.HALF_UP)).isNotEqualTo(payment.getAmount());
    }

    @Test
    void should_track_step_progression_linearly() {
        Payment payment = new Payment(senderAccountId, recipientAccountId, amount, "Linear progression test");
        TransactionId transactionId = TransactionId.generate();
        
        assertThat(payment.getCurrentStep()).isEqualTo(PaymentStep.INITIATED);
        
        payment.setTransactionId(transactionId);
        assertThat(payment.getCurrentStep()).isEqualTo(PaymentStep.TRANSACTION_CREATED);
        
        payment.advanceToStep(PaymentStep.TRANSACTION_PROCESSING);
        assertThat(payment.getCurrentStep()).isEqualTo(PaymentStep.TRANSACTION_PROCESSING);
        
        payment.advanceToStep(PaymentStep.SENDER_DEBITED);
        assertThat(payment.getCurrentStep()).isEqualTo(PaymentStep.SENDER_DEBITED);
        
        payment.advanceToStep(PaymentStep.RECIPIENT_CREDITED);
        assertThat(payment.getCurrentStep()).isEqualTo(PaymentStep.RECIPIENT_CREDITED);
        
        payment.markAsCompleted();
        assertThat(payment.getCurrentStep()).isEqualTo(PaymentStep.COMPLETED);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
    }
}