package com.corebank.payment.infrastructure.event;

import com.corebank.payment.infrastructure.outbox.PaymentEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@ConditionalOnProperty(name = "payment.events.kafka-enabled", havingValue = "true")
public class KafkaPaymentEventPublisher implements PaymentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaPaymentEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;

    public KafkaPaymentEventPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publish(UUID eventId, String eventType, String payload) {
        String topic = "payments.txn." + eventType.toLowerCase();
        log.info("NAMNM EVENT kafka-send eventId={} topic={}", eventId, topic);
        kafkaTemplate.send(topic, eventId.toString(), payload)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        log.error("NAMNM EVENT kafka-failed eventId={} message={}", eventId, throwable.getMessage(), throwable);
                    } else {
                        log.info("NAMNM EVENT kafka-success eventId={} partition={} offset={}",
                                eventId,
                                result != null ? result.getRecordMetadata().partition() : null,
                                result != null ? result.getRecordMetadata().offset() : null);
                    }
                });
    }
}
