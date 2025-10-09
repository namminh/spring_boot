package com.corebank.payment.stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class PaymentEventTransformer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventTransformer.class);

    private PaymentEventTransformer() {
    }

    static String transform(ObjectMapper objectMapper, String value) {
        try {
            JsonNode node = objectMapper.readTree(value);
            if (node instanceof com.fasterxml.jackson.databind.node.ObjectNode objectNode) {
                objectNode.put("processedAt", OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
                objectNode.put("amountMinor", node.path("amount").decimalValue().movePointRight(2));
                return objectMapper.writeValueAsString(objectNode);
            }
            return value;
        } catch (Exception ex) {
            log.warn("NAMNM STREAM transform failure payload={} message={}", value, ex.getMessage(), ex);
            return value;
        }
    }
}
