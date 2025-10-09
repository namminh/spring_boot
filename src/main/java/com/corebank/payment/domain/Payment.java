package com.corebank.payment.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record Payment(UUID id,
                      String reference,
                      BigDecimal amount,
                      String currency,
                      String channel,
                      PaymentStatus status,
                      OffsetDateTime createdAt,
                      OffsetDateTime lastUpdatedAt) implements Serializable {
}
