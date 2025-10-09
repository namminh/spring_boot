package com.corebank.payment.infrastructure.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.StructuredTaskScope;

@Component
class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxRepository outboxRepository;
    private final PaymentEventPublisher eventPublisher;

    OutboxRelay(OutboxRepository outboxRepository, PaymentEventPublisher eventPublisher) {
        this.outboxRepository = outboxRepository;
        this.eventPublisher = eventPublisher;
    }

    @Scheduled(fixedDelayString = "${payment.outbox.poll-interval-ms:2000}")
    @Transactional
    public void forwardPendingEvents() {
        List<OutboxEventEntity> events = outboxRepository.findTop20ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
        if (events.isEmpty()) {
            log.debug("NAMNM OUTBOX idle");
            return;
        }

        log.info("NAMNM OUTBOX dispatch batchSize={} firstEvent={}", events.size(), events.get(0).getEventId());

        Map<UUID, OutboxDispatchResult> results = dispatchInParallel(events);
        OffsetDateTime attemptTime = OffsetDateTime.now();

        events.forEach(event -> {
            OutboxDispatchResult result = results.get(event.getEventId());
            if (result == null) {
                log.warn("NAMNM OUTBOX missing result for eventId={} - marking FAILED", event.getEventId());
                event.setStatus(OutboxStatus.FAILED);
            } else {
                switch (result) {
                    case OutboxDispatchResult(UUID eventId, OutboxStatus status, String failureMessage)
                            when status == OutboxStatus.SENT -> {
                        event.setStatus(OutboxStatus.SENT);
                        log.info("NAMNM OUTBOX published eventId={}", eventId);
                    }
                    case OutboxDispatchResult(UUID eventId, OutboxStatus status, String failureMessage) -> {
                        event.setStatus(status);
                        log.warn("NAMNM OUTBOX publish failed eventId={} reason={}", eventId, failureMessage);
                    }
                }
            }
            event.setLastAttemptAt(attemptTime);
        });
    }

    private Map<UUID, OutboxDispatchResult> dispatchInParallel(List<OutboxEventEntity> events) {
        Map<UUID, OutboxDispatchResult> results = new HashMap<>();
        try (var scope = new StructuredTaskScope.ShutdownOnFailure(Thread.ofVirtual().factory())) {
            Map<UUID, StructuredTaskScope.Subtask<OutboxDispatchResult>> subtasks = new HashMap<>(events.size());
            for (OutboxEventEntity event : events) {
                OutboxDispatchCommand command = new OutboxDispatchCommand(event.getEventId(), event.getEventType(), event.getPayload());
                subtasks.put(event.getEventId(), scope.fork(() -> publish(command)));
            }
            scope.join();
            scope.throwIfFailed();
            subtasks.forEach((eventId, subtask) -> {
                try {
                    results.put(eventId, subtask.get());
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Outbox dispatch interrupted", interruptedException);
                } catch (ExecutionException executionException) {
                    throw new IllegalStateException("Outbox dispatch failed", executionException.getCause());
                }
            });
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Outbox dispatch interrupted", interruptedException);
        }
        return results;
    }

    private OutboxDispatchResult publish(OutboxDispatchCommand command) {
        try {
            log.info("NAMNM OUTBOX publish eventId={} type={}", command.eventId(), command.eventType());
            eventPublisher.publish(command.eventId(), command.eventType(), command.payload());
            return new OutboxDispatchResult(command.eventId(), OutboxStatus.SENT, null);
        } catch (Exception ex) {
            log.warn("NAMNM OUTBOX failed eventId={} message={}", command.eventId(), ex.getMessage(), ex);
            return new OutboxDispatchResult(command.eventId(), OutboxStatus.FAILED, ex.getMessage());
        }
    }

    private record OutboxDispatchCommand(UUID eventId, String eventType, String payload) {
    }

    private record OutboxDispatchResult(UUID eventId, OutboxStatus status, String failureMessage) {
    }
}
