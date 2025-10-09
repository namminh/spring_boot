package com.corebank.investigation.domain;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

public class InvestigationCase implements Serializable {

    private static final long serialVersionUID = 1L;

    private final UUID id;
    private final String reference;
    private final InvestigationStatus status;
    private final String reportedBy;
    private final String assignedTo;
    private final String lastNote;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;
    private final OffsetDateTime lastStatusChange;

    public InvestigationCase(UUID id,
                             String reference,
                             InvestigationStatus status,
                             String reportedBy,
                             String assignedTo,
                             String lastNote,
                             OffsetDateTime createdAt,
                             OffsetDateTime updatedAt,
                             OffsetDateTime lastStatusChange) {
        this.id = id;
        this.reference = reference;
        this.status = status;
        this.reportedBy = reportedBy;
        this.assignedTo = assignedTo;
        this.lastNote = lastNote;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.lastStatusChange = lastStatusChange;
    }

    public UUID getId() {
        return id;
    }

    public String getReference() {
        return reference;
    }

    public InvestigationStatus getStatus() {
        return status;
    }

    public String getReportedBy() {
        return reportedBy;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public String getLastNote() {
        return lastNote;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public OffsetDateTime getLastStatusChange() {
        return lastStatusChange;
    }
}
