package com.corebank.payment.application;

import com.corebank.payment.domain.PaymentCommand;
import com.corebank.payment.infrastructure.tuxedo.TuxedoClient;
import com.corebank.payment.infrastructure.tuxedo.TuxedoPaymentResponse;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
class TuxedoGateway {

    private final TuxedoClient tuxedoClient;
    private static final Logger log = LoggerFactory.getLogger(TuxedoGateway.class);

    TuxedoGateway(TuxedoClient tuxedoClient) {
        this.tuxedoClient = tuxedoClient;
    }

    @Retry(name = "tuxedo-process")
    TuxedoPaymentResponse process(PaymentCommand command) {
        log.info("NAMNM GWY invoke reference={} amount={} currency={}",
                command.reference(), command.amount(), command.currency());
        TuxedoPaymentResponse response = tuxedoClient.processPayment(command);
        log.info("NAMNM GWY response reference={} success={} code={} msg={}",
                command.reference(), response.success(), response.responseCode(), response.responseMessage());
        return response;
    }
}
