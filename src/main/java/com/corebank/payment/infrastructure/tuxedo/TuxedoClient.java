package com.corebank.payment.infrastructure.tuxedo;

import com.corebank.payment.domain.PaymentCommand;

public interface TuxedoClient {

    TuxedoPaymentResponse processPayment(PaymentCommand command);
}
