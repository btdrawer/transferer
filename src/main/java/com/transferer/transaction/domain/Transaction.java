package com.transferer.transaction.domain;

import com.transferer.account.domain.AccountId;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Table("transactions")
public class Transaction {

    @Id
    private TransactionId id;

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

    @Column("status")
    @NotNull
    private TransactionStatus status;

    @Column("description")
    private String description;

    @Column("created_at")
    @NotNull
    private LocalDateTime createdAt;

    @Column("updated_at")
    @NotNull
    private LocalDateTime updatedAt;

    @Column("completed_at")
    private LocalDateTime completedAt;

    protected Transaction() {
    }

    public Transaction(AccountId senderAccountId, AccountId recipientAccountId, BigDecimal amount, String description) {
        this.id = TransactionId.generate();
        this.senderAccountId = Objects.requireNonNull(senderAccountId, "Sender account ID cannot be null");
        this.recipientAccountId = Objects.requireNonNull(recipientAccountId, "Recipient account ID cannot be null");
        this.amount = Objects.requireNonNull(amount, "Amount cannot be null");
        this.description = description;
        this.status = TransactionStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transaction amount must be positive");
        }
        if (senderAccountId.equals(recipientAccountId)) {
            throw new IllegalArgumentException("Sender and recipient accounts cannot be the same");
        }
    }

    public void markAsProcessing() {
        if (status != TransactionStatus.PENDING) {
            throw new IllegalStateException("Can only mark pending transactions as processing");
        }
        this.status = TransactionStatus.PROCESSING;
        this.updatedAt = LocalDateTime.now();
    }

    public void markAsCompleted() {
        if (status != TransactionStatus.PROCESSING) {
            throw new IllegalStateException("Can only mark processing transactions as completed");
        }
        this.status = TransactionStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void markAsFailed() {
        if (status == TransactionStatus.COMPLETED) {
            throw new IllegalStateException("Cannot mark completed transactions as failed");
        }
        this.status = TransactionStatus.FAILED;
        this.updatedAt = LocalDateTime.now();
    }

    public TransactionId getId() {
        return id;
    }

    public String getIdValue() {
        return id != null ? id.getValue() : null;
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

    public TransactionStatus getStatus() {
        return status;
    }

    public String getDescription() {
        return description;
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
        Transaction that = (Transaction) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", senderAccountId=" + senderAccountId +
                ", recipientAccountId=" + recipientAccountId +
                ", amount=" + amount +
                ", status=" + status +
                ", description='" + description + '\'' +
                '}';
    }
}