package com.corebank.orchestrator.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "monitoring_alerts")
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "severity", nullable = false)
    private String severity;

    @Column(name = "message", nullable = false)
    private String message;

    @Column(name = "acknowledged", nullable = false)
    private boolean acknowledged;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public Alert() {
        // JPA constructor
    }

    public Alert(String severity, String message, boolean acknowledged, OffsetDateTime createdAt) {
        this.severity = severity;
        this.message = message;
        this.acknowledged = acknowledged;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isAcknowledged() {
        return acknowledged;
    }

    public void setAcknowledged(boolean acknowledged) {
        this.acknowledged = acknowledged;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
