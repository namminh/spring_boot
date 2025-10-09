package com.corebank.payment.infrastructure.tuxedo;

import com.corebank.payment.domain.PaymentCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import static java.util.concurrent.ThreadLocalRandom.current;

@Component
public class MockTuxedoClient implements TuxedoClient {

    private static final Logger log = LoggerFactory.getLogger(MockTuxedoClient.class);

    @Override
    public TuxedoPaymentResponse processPayment(PaymentCommand command) {
        simulateLatency();
        return switch (command) {
            case PaymentCommand(String reference,
                                BigDecimal amount,
                                String currency,
                                String channel,
                                String debtor,
                                String creditor) when amount.compareTo(new BigDecimal("500000000")) > 0 -> {
                log.warn("Tuxedo rejection for reference {} due to amount {}", reference, amount);
                yield TuxedoPaymentResponse.failure("LIMIT_EXCEEDED", "Amount too large for mock processing");
            }
            case PaymentCommand(String reference,
                                BigDecimal amount,
                                String currency,
                                String channel,
                                String debtor,
                                String creditor) when current().nextInt(100) < 5 -> {
                log.warn("Tuxedo mock random failure for reference {}", reference);
                yield TuxedoPaymentResponse.failure("RANDOM_FAIL", "Intermittent mock failure");
            }
            default -> TuxedoPaymentResponse.success("APPROVED");
        };
    }

    private void simulateLatency() {
        try {
            Thread.sleep(current().nextLong(20, 120));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
