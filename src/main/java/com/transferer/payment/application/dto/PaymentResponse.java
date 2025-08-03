package com.transferer.payment.application.dto;

import com.transferer.payment.domain.Payment;
import com.transferer.payment.domain.PaymentStatus;
import com.transferer.payment.domain.PaymentStep;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentResponse {

    private String id;
    private String transactionId;
    private String senderAccountId;
    private String recipientAccountId;
    private BigDecimal amount;
    private String description;
    private PaymentStatus status;
    private PaymentStep currentStep;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;

    public PaymentResponse() {
    }

    public PaymentResponse(Payment payment) {
        this.id = payment.getId().getValue();
        this.transactionId = payment.getTransactionId() != null ? payment.getTransactionId().getValue() : null;
        this.senderAccountId = payment.getSenderAccountId().getValue();
        this.recipientAccountId = payment.getRecipientAccountId().getValue();
        this.amount = payment.getAmount();
        this.description = payment.getDescription();
        this.status = payment.getStatus();
        this.currentStep = payment.getCurrentStep();
        this.failureReason = payment.getFailureReason();
        this.createdAt = payment.getCreatedAt();
        this.updatedAt = payment.getUpdatedAt();
        this.completedAt = payment.getCompletedAt();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getSenderAccountId() {
        return senderAccountId;
    }

    public void setSenderAccountId(String senderAccountId) {
        this.senderAccountId = senderAccountId;
    }

    public String getRecipientAccountId() {
        return recipientAccountId;
    }

    public void setRecipientAccountId(String recipientAccountId) {
        this.recipientAccountId = recipientAccountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public PaymentStep getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(PaymentStep currentStep) {
        this.currentStep = currentStep;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}