package com.corebank.payment.infrastructure.event;

import com.corebank.payment.infrastructure.outbox.PaymentEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@ConditionalOnProperty(name = "payment.events.kafka-enabled", havingValue = "false", matchIfMissing = true)
public class LoggingPaymentEventPublisher implements PaymentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingPaymentEventPublisher.class);

    @Override
    public void publish(UUID eventId, String eventType, String payload) {
        log.info("NAMNM EVENT fallback eventId={} type={} payload={}", eventId, eventType, payload);
    }
}
