package com.corebank.orchestrator.orchestrator;

import com.corebank.orchestrator.adapter.AlertRepository;
import com.corebank.orchestrator.adapter.PaymentTransactionRepository;
import com.corebank.orchestrator.domain.Alert;
import com.corebank.orchestrator.domain.CoreStatusSnapshot;
import com.corebank.orchestrator.domain.PaymentTransaction;
import com.corebank.orchestrator.event.MonitoringEventPublisher;
import java.time.OffsetDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MonitoringOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(MonitoringOrchestrator.class);

    private final PaymentTransactionRepository transactionRepository;
    private final AlertRepository alertRepository;
    private final MonitoringEventPublisher eventPublisher;

    public MonitoringOrchestrator(PaymentTransactionRepository transactionRepository,
                                  AlertRepository alertRepository,
                                  MonitoringEventPublisher eventPublisher) {
        this.transactionRepository = transactionRepository;
        this.alertRepository = alertRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public CoreStatusSnapshot snapshotCoreStatus() {
        long pending = transactionRepository.countByStatus("PENDING");
        long failed = transactionRepository.countByStatus("FAILED");
        long totalActiveAlerts = alertRepository.countByAcknowledgedFalse();

        log.info("NAMNM ORCH snapshot counts pending={} failed={} activeAlerts={}",
            pending, failed, totalActiveAlerts);

        String status = failed > 0 ? "DEGRADED" : pending > 5 ? "WARNING" : "HEALTHY";
        String message = switch (status) {
            case "DEGRADED" -> "Detected failed transactions pending remediation";
            case "WARNING" -> "Pending queue backlog requires observation";
            default -> "Core payment engine operating within normal parameters";
        };

        if (totalActiveAlerts > 0 && "HEALTHY".equals(status)) {
            status = "WARNING";
            message = "Active alerts require follow-up despite healthy transaction metrics";
        }

        CoreStatusSnapshot snapshot = new CoreStatusSnapshot(status, message, OffsetDateTime.now());
        log.info("NAMNM ORCH snapshot result status={} message={} timestamp={}",
            snapshot.status(), snapshot.message(), snapshot.collectedAt());
        return snapshot;
    }

    @Transactional(readOnly = true)
    public List<PaymentTransaction> loadRecentTransactions(OffsetDateTime since) {
        log.info("NAMNM ORCH load-recent-transactions since={}", since);
        List<PaymentTransaction> transactions = transactionRepository.findTop20ByProcessedAtAfterOrderByProcessedAtDesc(since);
        log.info("NAMNM ORCH load-recent-transactions resultCount={}", transactions.size());
        return transactions;
    }

    @Transactional(readOnly = true)
    public List<Alert> loadActiveAlerts() {
        log.info("NAMNM ORCH load-active-alerts");
        List<Alert> alerts = alertRepository.findByAcknowledgedFalseOrderByCreatedAtDesc();
        log.info("NAMNM ORCH load-active-alerts resultCount={}", alerts.size());
        return alerts;
    }

    public Alert recordManualAlert(Alert alert) {
        log.info("NAMNM ORCH record-manual-alert severity={} message={}", alert.getSeverity(), alert.getMessage());
        Alert saved = alertRepository.save(alert);
        eventPublisher.publishAlertCreated(saved);
        log.info("NAMNM ORCH record-manual-alert saved id={} acknowledged={}", saved.getId(), saved.isAcknowledged());
        return saved;
    }
}
