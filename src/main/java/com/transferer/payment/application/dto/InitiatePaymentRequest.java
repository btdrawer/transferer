package com.transferer.payment.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class InitiatePaymentRequest {

    @NotBlank(message = "Sender account ID is required")
    private String senderAccountId;

    @NotBlank(message = "Recipient account ID is required")
    private String recipientAccountId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", inclusive = true, message = "Amount must be positive")
    private BigDecimal amount;

    private String description;

    public InitiatePaymentRequest() {
    }

    public InitiatePaymentRequest(String senderAccountId, String recipientAccountId, BigDecimal amount, String description) {
        this.senderAccountId = senderAccountId;
        this.recipientAccountId = recipientAccountId;
        this.amount = amount;
        this.description = description;
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
}