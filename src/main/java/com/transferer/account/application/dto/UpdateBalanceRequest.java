package com.transferer.account.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class UpdateBalanceRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be positive")
    private BigDecimal amount;

    @NotNull(message = "Operation type is required")
    private BalanceOperation operation;

    public UpdateBalanceRequest() {
    }

    public UpdateBalanceRequest(BigDecimal amount, BalanceOperation operation) {
        this.amount = amount;
        this.operation = operation;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BalanceOperation getOperation() {
        return operation;
    }

    public void setOperation(BalanceOperation operation) {
        this.operation = operation;
    }

    public enum BalanceOperation {
        CREDIT, DEBIT
    }
}