package com.corebank.orchestrator;

import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class CorebankOrchestratorApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(CorebankOrchestratorApplication.class);
        application.setDefaultProperties(Map.of("spring.application.name", "corebank-orchestrator"));
        application.run(args);
    }
}
