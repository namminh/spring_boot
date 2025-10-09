package com.corebank.orchestrator.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CreateAlertRequest {

    @NotBlank
    @Pattern(regexp = "INFO|WARN|CRITICAL", message = "severity must be INFO, WARN, or CRITICAL")
    private String severity;

    @NotBlank
    @Size(max = 512)
    private String message;

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
}
