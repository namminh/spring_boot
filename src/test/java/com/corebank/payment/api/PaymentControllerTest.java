package com.corebank.payment.api;

import com.corebank.payment.TestPaymentConfig;
import com.corebank.payment.api.dto.PaymentRequestDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(TestPaymentConfig.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void initiatePaymentAndFetchStatus() throws Exception {
        PaymentRequestDto request = new PaymentRequestDto(
                "REF-001",
                new BigDecimal("1000.00"),
                "VND",
                "MOBILE",
                "1234567890",
                "0987654321");

        mockMvc.perform(post("/api/v1/payments")
                        .with(httpBasic("orchestrator", "changeme"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/payments/REF-001"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mockMvc.perform(get("/api/v1/payments/REF-001")
                        .with(httpBasic("orchestrator", "changeme")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mockMvc.perform(get("/api/v1/payments/metrics/status-count")
                        .with(httpBasic("orchestrator", "changeme")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.COMPLETED").value(1));
    }
}
