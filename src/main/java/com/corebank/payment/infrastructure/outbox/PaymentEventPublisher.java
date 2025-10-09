package com.corebank.payment.infrastructure.outbox;

import java.util.UUID;

public interface PaymentEventPublisher {

    void publish(UUID eventId, String eventType, String payload);
}
