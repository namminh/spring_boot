package com.corebank.payment.infrastructure.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
public class OutboxEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Lob
    @Column(nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "last_attempt_at")
    private OffsetDateTime lastAttemptAt;

    @Version
    private Long version;

    public static OutboxEventEntity pending(UUID aggregateId, String eventType, String payload) {
        OutboxEventEntity entity = new OutboxEventEntity();
        entity.eventId = UUID.randomUUID();
        entity.aggregateId = aggregateId;
        entity.eventType = eventType;
        entity.payload = payload;
        entity.status = OutboxStatus.PENDING;
        entity.createdAt = OffsetDateTime.now();
        return entity;
    }

    public Long getId() {
        return id;
    }

    public UUID getEventId() {
        return eventId;
    }

    public UUID getAggregateId() {
        return aggregateId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPayload() {
        return payload;
    }

    public OutboxStatus getStatus() {
        return status;
    }

    public void setStatus(OutboxStatus status) {
        this.status = status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getLastAttemptAt() {
        return lastAttemptAt;
    }

    public void setLastAttemptAt(OffsetDateTime lastAttemptAt) {
        this.lastAttemptAt = lastAttemptAt;
    }
}
