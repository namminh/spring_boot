package com.corebank.investigation.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableConfigurationProperties(InvestigationProperties.class)
public class InvestigationConfig {

    @Bean
    @ConditionalOnProperty(prefix = "spring.cloud.discovery", name = "enabled", havingValue = "true")
    @LoadBalanced
    RestTemplate discoveryEnabledRestTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }

    @Bean
    @ConditionalOnMissingBean(RestTemplate.class)
    RestTemplate defaultRestTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }
}
