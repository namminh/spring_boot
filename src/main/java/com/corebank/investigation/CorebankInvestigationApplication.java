package com.corebank.investigation;

import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class CorebankInvestigationApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(CorebankInvestigationApplication.class);
        application.setDefaultProperties(Map.of("spring.application.name", "corebank-investigation"));
        application.run(args);
    }
}
