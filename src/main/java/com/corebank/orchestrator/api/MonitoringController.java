package com.corebank.orchestrator.api;

import com.corebank.orchestrator.domain.Alert;
import com.corebank.orchestrator.domain.CoreStatusSnapshot;
import com.corebank.orchestrator.domain.PaymentTransaction;
import com.corebank.orchestrator.orchestrator.MonitoringOrchestrator;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/monitoring")
public class MonitoringController {

    private static final Logger log = LoggerFactory.getLogger(MonitoringController.class);

    private final MonitoringOrchestrator orchestrator;

    public MonitoringController(MonitoringOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @GetMapping("/status")
    public CoreStatusSnapshot status() {
        log.info("NAMNM ORCH status requested");
        CoreStatusSnapshot snapshot = orchestrator.snapshotCoreStatus();
        log.info("NAMNM ORCH status result={} message={} timestamp={}",
            snapshot.status(), snapshot.message(), snapshot.collectedAt());
        return snapshot;
    }

    @GetMapping("/transactions/recent")
    public List<PaymentTransaction> recentTransactions(@RequestParam(defaultValue = "4") int hours) {
        int boundedHours = Math.min(Math.max(hours, 1), 24);
        log.info("NAMNM ORCH recent-transactions requested hours={} bounded={}", hours, boundedHours);
        OffsetDateTime since = OffsetDateTime.now().minusHours(boundedHours);
        List<PaymentTransaction> transactions = orchestrator.loadRecentTransactions(since);
        log.info("NAMNM ORCH recent-transactions resultCount={}", transactions.size());
        return transactions;
    }

    @GetMapping("/alerts/active")
    public List<Alert> activeAlerts() {
        log.info("NAMNM ORCH active-alerts requested");
        List<Alert> alerts = orchestrator.loadActiveAlerts();
        log.info("NAMNM ORCH active-alerts resultCount={}", alerts.size());
        return alerts;
    }

    @PostMapping("/alerts")
    public ResponseEntity<Alert> createAlert(@Valid @RequestBody CreateAlertRequest request) {
        log.info("NAMNM ORCH create-alert requested severity={} message={}", request.getSeverity(), request.getMessage());
        Alert alert = new Alert();
        alert.setSeverity(request.getSeverity().toUpperCase());
        alert.setMessage(request.getMessage());
        alert.setAcknowledged(false);
        alert.setCreatedAt(OffsetDateTime.now());

        Alert saved = orchestrator.recordManualAlert(alert);
        log.info("NAMNM ORCH create-alert created id={} severity={} acknowledged={}",
            saved.getId(), saved.getSeverity(), saved.isAcknowledged());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
}
