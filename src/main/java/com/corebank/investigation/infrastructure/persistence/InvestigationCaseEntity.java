package com.corebank.investigation.infrastructure.persistence;

import com.corebank.investigation.domain.InvestigationCase;
import com.corebank.investigation.domain.InvestigationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "investigation_case")
public class InvestigationCaseEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String reference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvestigationStatus status;

    @Column(name = "reported_by", nullable = false)
    private String reportedBy;

    @Column(name = "assigned_to")
    private String assignedTo;

    @Column(name = "last_note", length = 4000)
    private String lastNote;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "status_changed_at", nullable = false)
    private OffsetDateTime statusChangedAt;

    public static InvestigationCaseEntity fromDomain(InvestigationCase investigationCase) {
        InvestigationCaseEntity entity = new InvestigationCaseEntity();
        entity.id = investigationCase.getId();
        entity.reference = investigationCase.getReference();
        entity.status = investigationCase.getStatus();
        entity.reportedBy = investigationCase.getReportedBy();
        entity.assignedTo = investigationCase.getAssignedTo();
        entity.lastNote = investigationCase.getLastNote();
        entity.createdAt = investigationCase.getCreatedAt();
        entity.updatedAt = investigationCase.getUpdatedAt();
        entity.statusChangedAt = investigationCase.getLastStatusChange();
        return entity;
    }

    public InvestigationCase toDomain() {
        return new InvestigationCase(
            id,
            reference,
            status,
            reportedBy,
            assignedTo,
            lastNote,
            createdAt,
            updatedAt,
            statusChangedAt
        );
    }

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (statusChangedAt == null) {
            statusChangedAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public InvestigationStatus getStatus() {
        return status;
    }

    public void setStatus(InvestigationStatus status) {
        this.status = status;
    }

    public String getReportedBy() {
        return reportedBy;
    }

    public void setReportedBy(String reportedBy) {
        this.reportedBy = reportedBy;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }

    public String getLastNote() {
        return lastNote;
    }

    public void setLastNote(String lastNote) {
        this.lastNote = lastNote;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public OffsetDateTime getStatusChangedAt() {
        return statusChangedAt;
    }

    public void setStatusChangedAt(OffsetDateTime statusChangedAt) {
        this.statusChangedAt = statusChangedAt;
    }
}
