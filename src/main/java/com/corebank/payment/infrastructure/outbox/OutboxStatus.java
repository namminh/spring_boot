package com.corebank.payment.infrastructure.outbox;

public enum OutboxStatus {
    PENDING,
    SENT,
    FAILED
}
