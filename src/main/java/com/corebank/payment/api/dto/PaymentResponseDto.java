package com.corebank.payment.api.dto;

import com.corebank.payment.domain.PaymentStatus;

import java.util.UUID;

public record PaymentResponseDto(UUID paymentId,
                                 String reference,
                                 PaymentStatus status) {
}
