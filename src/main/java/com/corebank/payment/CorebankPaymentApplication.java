package com.corebank.payment;

import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableDiscoveryClient
public class CorebankPaymentApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(CorebankPaymentApplication.class);
        application.setDefaultProperties(Map.of("spring.application.name", "corebank-payment"));
        application.run(args);
    }
}
