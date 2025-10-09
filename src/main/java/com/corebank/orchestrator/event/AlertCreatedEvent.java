package com.corebank.orchestrator.event;

import com.corebank.orchestrator.domain.Alert;

public class AlertCreatedEvent {

    private final Alert alert;

    public AlertCreatedEvent(Alert alert) {
        this.alert = alert;
    }

    public Alert getAlert() {
        return alert;
    }
}
