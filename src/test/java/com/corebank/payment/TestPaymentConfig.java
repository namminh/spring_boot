package com.corebank.payment;

import com.corebank.payment.domain.PaymentCommand;
import com.corebank.payment.infrastructure.outbox.PaymentEventPublisher;
import com.corebank.payment.infrastructure.tuxedo.TuxedoClient;
import com.corebank.payment.infrastructure.tuxedo.TuxedoPaymentResponse;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestPaymentConfig {

    @Bean
    @Primary
    public TuxedoClient testTuxedoClient() {
        return new DeterministicTuxedoClient();
    }

    @Bean
    @Primary
    public PaymentEventPublisher testPaymentEventPublisher() {
        return (eventId, eventType, payload) -> {
            // no-op publisher for tests
        };
    }

    private static final class DeterministicTuxedoClient implements TuxedoClient {
        @Override
        public TuxedoPaymentResponse processPayment(PaymentCommand command) {
            return TuxedoPaymentResponse.success("APPROVED");
        }
    }
}
