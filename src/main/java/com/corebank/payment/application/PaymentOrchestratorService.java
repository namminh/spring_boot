package com.corebank.payment.application;

import com.corebank.payment.domain.Payment;
import com.corebank.payment.domain.PaymentCommand;
import com.corebank.payment.domain.PaymentStatus;
import com.corebank.payment.infrastructure.outbox.OutboxEventEntity;
import com.corebank.payment.infrastructure.outbox.OutboxRepository;
import com.corebank.payment.infrastructure.persistence.PaymentEntity;
import com.corebank.payment.infrastructure.persistence.PaymentRepository;
import com.corebank.payment.infrastructure.tuxedo.TuxedoPaymentResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(PaymentOrchestratorService.class);

    private final PaymentRepository paymentRepository;
    private final OutboxRepository outboxRepository;
    private final TuxedoGateway tuxedoGateway;
    private final ObjectMapper objectMapper;

    public PaymentOrchestratorService(PaymentRepository paymentRepository,
                                      OutboxRepository outboxRepository,
                                      TuxedoGateway tuxedoGateway,
                                      ObjectMapper objectMapper) {
        this.paymentRepository = paymentRepository;
        this.outboxRepository = outboxRepository;
        this.tuxedoGateway = tuxedoGateway;
        this.objectMapper = objectMapper;
    }

    @Transactional
    @CachePut(cacheNames = "paymentByReference", key = "#result.reference")
    @CacheEvict(cacheNames = "paymentStatusCount", allEntries = true)
    public Payment orchestrate(PaymentCommand command) {
        log.info("NAMNM SRV orchestrate start reference={} channel={} amount={} currency={}",
                command.reference(), command.channel(), command.amount(), command.currency());
        PaymentEntity entity = new PaymentEntity();
        entity.setId(UUID.randomUUID());
        entity.setReference(command.reference());
        entity.setAmount(command.amount());
        entity.setCurrency(command.currency());
        entity.setChannel(command.channel());
        entity.setStatus(PaymentStatus.RECEIVED);
        entity.setDebtorAccount(command.debtorAccount());
        entity.setCreditorAccount(command.creditorAccount());
        entity = paymentRepository.save(entity);

        entity.setStatus(PaymentStatus.IN_PROGRESS);
        entity = paymentRepository.save(entity);

        TuxedoPaymentResponse response;
        try {
            response = tuxedoGateway.process(command);
        } catch (Exception ex) {
            log.error("NAMNM SRV tuxedo exception reference={} message={}", command.reference(), ex.getMessage(), ex);
            response = TuxedoPaymentResponse.failure("EXCEPTION", ex.getMessage());
        }

        PaymentStatus targetStatus = switch (response) {
            case TuxedoPaymentResponse(boolean success, var ignoredCode, var ignoredMessage) when success -> PaymentStatus.COMPLETED;
            case TuxedoPaymentResponse(boolean success, var ignoredCode, var ignoredMessage) -> PaymentStatus.FAILED;
        };
        entity.setStatus(targetStatus);
        entity = paymentRepository.save(entity);

        storeOutboxEvent(entity, response);

        log.info("NAMNM SRV orchestrate end reference={} paymentId={} status={} code={} msg={}",
                entity.getReference(), entity.getId(), entity.getStatus(), response.responseCode(), response.responseMessage());
        return entity.toDomain();
    }

    @Cacheable(cacheNames = "paymentByReference", key = "#reference", unless = "#result.isEmpty()")
    public Optional<Payment> findByReference(String reference) {
        return paymentRepository.findByReference(reference)
                .map(PaymentEntity::toDomain);
    }

    @Cacheable(cacheNames = "paymentStatusCount", key = "#status")
    public long countByStatus(PaymentStatus status) {
        return paymentRepository.countByStatus(status);
    }

    private void storeOutboxEvent(PaymentEntity entity, TuxedoPaymentResponse response) {
        String payload = serializePayload(entity, response);
        OutboxEventEntity outboxEvent = OutboxEventEntity.pending(entity.getId(),
                eventType(response),
                payload);
        outboxRepository.save(outboxEvent);
        log.info("NAMNM SRV outbox stored paymentId={} eventId={} type={}",
                entity.getId(), outboxEvent.getEventId(), outboxEvent.getEventType());
    }

    private String eventType(TuxedoPaymentResponse response) {
        return switch (response) {
            case TuxedoPaymentResponse(boolean success, var ignoredCode, var ignoredMessage) when success -> "COMPLETED";
            case TuxedoPaymentResponse(boolean success, var ignoredCode, var ignoredMessage) -> "FAILED";
        };
    }

    private String serializePayload(PaymentEntity entity, TuxedoPaymentResponse response) {
        try {
            return objectMapper.writeValueAsString(new PaymentEventPayload(entity.getId(),
                    entity.getReference(),
                    entity.getAmount(),
                    entity.getCurrency(),
                    entity.getStatus(),
                    response.responseCode(),
                    response.responseMessage()));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Payment payload serialization failed", e);
        }
    }

    private record PaymentEventPayload(UUID paymentId,
                                       String reference,
                                       java.math.BigDecimal amount,
                                       String currency,
                                       PaymentStatus status,
                                       String responseCode,
                                       String responseMessage) {
    }
}
