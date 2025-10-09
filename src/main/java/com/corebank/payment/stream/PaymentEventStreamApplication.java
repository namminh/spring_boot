package com.corebank.payment.stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.streams.StreamsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;

@SpringBootApplication
@EnableDiscoveryClient
public class PaymentEventStreamApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(PaymentEventStreamApplication.class);
        application.setDefaultProperties(Map.of("spring.application.name", "corebank-payment-stream"));
        application.run(args);
    }

    @Bean
    @ConditionalOnProperty(name = "payment.streams.enabled", havingValue = "true")
    ApplicationRunner streamRunner(PaymentEventStreamProcessor processor) {
        return args -> processor.buildAndStart();
    }
}

@Configuration
@EnableKafkaStreams
@ConditionalOnProperty(name = "payment.streams.enabled", havingValue = "true")
class PaymentStreamConfig {

    private static final Logger log = LoggerFactory.getLogger(PaymentStreamConfig.class);

    @Bean
    PaymentEventStreamProcessor paymentEventStreamProcessor(ObjectMapper objectMapper, StreamsConfig streamsConfig) {
        Map<String, Object> config = new HashMap<>(streamsConfig.originals());
        log.info("NAMNM STREAM config {}", config);
        return new PaymentEventStreamProcessor(objectMapper, config);
    }
}
