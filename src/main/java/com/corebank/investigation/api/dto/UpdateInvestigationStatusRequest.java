package com.corebank.investigation.api.dto;

import com.corebank.investigation.domain.InvestigationStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateInvestigationStatusRequest(
    @NotNull InvestigationStatus status,
    String note,
    String assignedTo
) {
}
