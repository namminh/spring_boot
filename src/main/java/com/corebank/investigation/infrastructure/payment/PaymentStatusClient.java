package com.corebank.investigation.infrastructure.payment;

import com.corebank.investigation.config.InvestigationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class PaymentStatusClient {

    private static final Logger log = LoggerFactory.getLogger(PaymentStatusClient.class);

    private final RestTemplate restTemplate;
    private final InvestigationProperties properties;

    public PaymentStatusClient(RestTemplate restTemplate, InvestigationProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    public PaymentStatusSnapshot fetchStatus(String reference) {
        String url = properties.getPaymentService().getBaseUrl() + "/api/v1/payments/" + reference;
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object status = response.getBody().get("status");
                return new PaymentStatusSnapshot(true, status != null ? status.toString() : null, null);
            }
            return new PaymentStatusSnapshot(false, null, "Unexpected payment response status: " + response.getStatusCode());
        } catch (Exception ex) {
            log.warn("NAMNM INV payment-status lookup failed reference={} message={} ", reference, ex.getMessage());
            return new PaymentStatusSnapshot(false, null, ex.getMessage());
        }
    }

    public record PaymentStatusSnapshot(boolean success, String status, String errorMessage) {
    }
}
