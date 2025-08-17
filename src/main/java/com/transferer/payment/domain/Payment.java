package com.transferer.payment.domain;

import com.transferer.account.domain.AccountId;
import com.transferer.transaction.domain.TransactionId;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Table("payments")
public class Payment {

    @Id
    private PaymentId id;

    @Column("transaction_id")
    private TransactionId transactionId;

    @Column("sender_account_id")
    @NotNull
    private AccountId senderAccountId;

    @Column("recipient_account_id")
    @NotNull
    private AccountId recipientAccountId;

    @Column("amount")
    @NotNull
    @DecimalMin(value = "0.01", inclusive = true)
    private BigDecimal amount;

    @Column("description")
    private String description;

    @Column("status")
    @NotNull
    private PaymentStatus status;

    @Column("current_step")
    @NotNull
    private PaymentStep currentStep;

    @Column("failure_reason")
    private String failureReason;

    @Column("created_at")
    @NotNull
    private LocalDateTime createdAt;

    @Column("updated_at")
    @NotNull
    private LocalDateTime updatedAt;

    @Column("completed_at")
    private LocalDateTime completedAt;

    protected Payment() {
    }

    public Payment(AccountId senderAccountId, AccountId recipientAccountId, BigDecimal amount, String description) {
        this.id = PaymentId.generate();
        this.senderAccountId = Objects.requireNonNull(senderAccountId, "Sender account ID cannot be null");
        this.recipientAccountId = Objects.requireNonNull(recipientAccountId, "Recipient account ID cannot be null");
        this.amount = Objects.requireNonNull(amount, "Amount cannot be null");
        this.description = description;
        this.status = PaymentStatus.PENDING;
        this.currentStep = PaymentStep.INITIATED;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be positive");
        }
        if (senderAccountId.equals(recipientAccountId)) {
            throw new IllegalArgumentException("Sender and recipient accounts cannot be the same");
        }
    }

    public void startProcessing() {
        if (status != PaymentStatus.PENDING) {
            throw new IllegalStateException("Can only start processing pending payments");
        }
        this.status = PaymentStatus.PROCESSING;
        this.updatedAt = LocalDateTime.now();
    }

    public void advanceToStep(PaymentStep step) {
        this.currentStep = step;
        this.updatedAt = LocalDateTime.now();
    }

    public void setTransactionId(TransactionId transactionId) {
        if (this.transactionId != null) {
            throw new IllegalStateException("Transaction ID is already set");
        }
        this.transactionId = transactionId;
        this.currentStep = PaymentStep.TRANSACTION_CREATED;
        this.updatedAt = LocalDateTime.now();
    }

    public void markAsCompleted() {
        if (status == PaymentStatus.FAILED) {
            throw new IllegalStateException("Cannot mark failed payment as completed");
        }
        if (status == PaymentStatus.COMPENSATING) {
            throw new IllegalStateException("Cannot mark compensating payment as completed");
        }
        this.status = PaymentStatus.COMPLETED;
        this.currentStep = PaymentStep.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void markAsFailed(String reason) {
        this.status = PaymentStatus.FAILED;
        this.currentStep = PaymentStep.FAILED;
        this.failureReason = reason;
        this.updatedAt = LocalDateTime.now();
    }

    public void startCompensation() {
        if (status != PaymentStatus.PROCESSING) {
            throw new IllegalStateException("Can only start compensation for processing payments");
        }
        this.status = PaymentStatus.COMPENSATING;
        this.currentStep = PaymentStep.COMPENSATING_SENDER_CREDIT;
        this.updatedAt = LocalDateTime.now();
    }

    public void markAsCompensated() {
        if (status != PaymentStatus.COMPENSATING) {
            throw new IllegalStateException("Can only mark compensating payments as compensated");
        }
        this.status = PaymentStatus.FAILED;
        this.currentStep = PaymentStep.COMPENSATED;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean requiresCompensation() {
        return status == PaymentStatus.PROCESSING && 
               (currentStep == PaymentStep.SENDER_DEBITED || currentStep == PaymentStep.RECIPIENT_CREDITED);
    }

    public PaymentId getId() {
        return id;
    }

    public String getIdValue() {
        return id != null ? id.getValue() : null;
    }

    public TransactionId getTransactionId() {
        return transactionId;
    }

    public AccountId getSenderAccountId() {
        return senderAccountId;
    }

    public AccountId getRecipientAccountId() {
        return recipientAccountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public PaymentStep getCurrentStep() {
        return currentStep;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Payment payment = (Payment) o;
        return Objects.equals(id, payment.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Payment{" +
                "id=" + id +
                ", transactionId=" + transactionId +
                ", senderAccountId=" + senderAccountId +
                ", recipientAccountId=" + recipientAccountId +
                ", amount=" + amount +
                ", status=" + status +
                ", currentStep=" + currentStep +
                '}';
    }
}