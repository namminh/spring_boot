package com.corebank.investigation.api.dto;

import com.corebank.investigation.domain.InvestigationCase;
import com.corebank.investigation.domain.InvestigationStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record InvestigationResponse(
    UUID caseId,
    String reference,
    InvestigationStatus status,
    String reportedBy,
    String assignedTo,
    String lastNote,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    OffsetDateTime lastStatusChange
) {
    public static InvestigationResponse fromDomain(InvestigationCase investigationCase) {
        return new InvestigationResponse(
            investigationCase.getId(),
            investigationCase.getReference(),
            investigationCase.getStatus(),
            investigationCase.getReportedBy(),
            investigationCase.getAssignedTo(),
            investigationCase.getLastNote(),
            investigationCase.getCreatedAt(),
            investigationCase.getUpdatedAt(),
            investigationCase.getLastStatusChange()
        );
    }
}
