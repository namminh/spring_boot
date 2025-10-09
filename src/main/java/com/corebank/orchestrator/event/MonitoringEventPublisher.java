package com.corebank.orchestrator.event;

import com.corebank.orchestrator.domain.Alert;

public interface MonitoringEventPublisher {

    void publishAlertCreated(Alert alert);
}
