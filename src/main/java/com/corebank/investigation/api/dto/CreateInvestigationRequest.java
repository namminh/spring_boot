package com.corebank.investigation.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateInvestigationRequest(
    @NotBlank String reference,
    @NotBlank String reportedBy,
    String initialNote
) {
}
