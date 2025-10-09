package com.corebank.orchestrator.domain;

import java.time.OffsetDateTime;

public record CoreStatusSnapshot(String status, String message, OffsetDateTime collectedAt) {
}
