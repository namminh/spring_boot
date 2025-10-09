package com.corebank.payment.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PaymentRequestDto(@NotBlank String reference,
                                @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
                                @NotBlank String currency,
                                @NotBlank String channel,
                                @NotBlank String debtorAccount,
                                @NotBlank String creditorAccount) {
}
