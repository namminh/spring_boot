package com.corebank.investigation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "investigation")
public class InvestigationProperties {

    private final PaymentService paymentService = new PaymentService();
    private String defaultAssignee = "ops-investigation";

    public PaymentService getPaymentService() {
        return paymentService;
    }

    public String getDefaultAssignee() {
        return defaultAssignee;
    }

    public void setDefaultAssignee(String defaultAssignee) {
        this.defaultAssignee = defaultAssignee;
    }

    public static class PaymentService {
        private String baseUrl = "http://localhost:8080";

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
    }
}
