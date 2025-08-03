package com.transferer.payment.domain;

import jakarta.validation.constraints.NotNull;

import java.util.Objects;
import java.util.UUID;

public class PaymentId {

    @NotNull
    private String value;

    protected PaymentId() {
    }

    private PaymentId(String value) {
        this.value = Objects.requireNonNull(value, "Payment ID cannot be null");
    }

    public static PaymentId generate() {
        return new PaymentId(UUID.randomUUID().toString());
    }

    public static PaymentId of(String value) {
        return new PaymentId(value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaymentId paymentId = (PaymentId) o;
        return Objects.equals(value, paymentId.value);
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