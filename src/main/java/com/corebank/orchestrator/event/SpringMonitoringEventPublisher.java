package com.corebank.orchestrator.event;

import com.corebank.orchestrator.domain.Alert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class SpringMonitoringEventPublisher implements MonitoringEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(SpringMonitoringEventPublisher.class);
    private final ApplicationEventPublisher publisher;

    public SpringMonitoringEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publishAlertCreated(Alert alert) {
        publisher.publishEvent(new AlertCreatedEvent(alert));
        log.info("Published alert creation event for alert id={} severity={}", alert.getId(), alert.getSeverity());
    }
}
