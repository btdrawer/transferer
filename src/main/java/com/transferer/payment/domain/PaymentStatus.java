package com.transferer.payment.domain;

public enum PaymentStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    COMPENSATING
}