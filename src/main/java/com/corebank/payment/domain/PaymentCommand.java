package com.corebank.payment.domain;

import java.math.BigDecimal;

public record PaymentCommand(String reference,
                             BigDecimal amount,
                             String currency,
                             String channel,
                             String debtorAccount,
                             String creditorAccount) {
}
