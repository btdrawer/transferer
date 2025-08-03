package com.transferer.transaction.domain;

import jakarta.validation.constraints.NotNull;

import java.util.Objects;
import java.util.UUID;

public class TransactionId {

    @NotNull
    private String value;

    protected TransactionId() {
    }

    private TransactionId(String value) {
        this.value = Objects.requireNonNull(value, "Transaction ID cannot be null");
    }

    public static TransactionId generate() {
        return new TransactionId(UUID.randomUUID().toString());
    }

    public static TransactionId of(String value) {
        return new TransactionId(value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransactionId that = (TransactionId) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}