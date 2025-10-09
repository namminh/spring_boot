package com.corebank.investigation.application;

import com.corebank.investigation.config.InvestigationProperties;
import com.corebank.investigation.domain.InvestigationCase;
import com.corebank.investigation.domain.InvestigationStatus;
import com.corebank.investigation.infrastructure.payment.PaymentStatusClient;
import com.corebank.investigation.infrastructure.persistence.InvestigationCaseEntity;
import com.corebank.investigation.infrastructure.persistence.InvestigationCaseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class InvestigationService {

    private static final Logger log = LoggerFactory.getLogger(InvestigationService.class);

    private final InvestigationCaseRepository repository;
    private final PaymentStatusClient paymentStatusClient;
    private final InvestigationProperties properties;

    public InvestigationService(InvestigationCaseRepository repository,
                                PaymentStatusClient paymentStatusClient,
                                InvestigationProperties properties) {
        this.repository = repository;
        this.paymentStatusClient = paymentStatusClient;
        this.properties = properties;
    }

    public InvestigationCase createCase(String reference, String reportedBy, String initialNote) {
        Optional<InvestigationCaseEntity> existing = repository.findByReference(reference);
        if (existing.isPresent()) {
            log.info("NAMNM INV case existing reference={} caseId={}", reference, existing.get().getId());
            return existing.get().toDomain();
        }

        OffsetDateTime now = OffsetDateTime.now();
        UUID caseId = UUID.randomUUID();
        InvestigationStatus status = InvestigationStatus.OPEN;

        InvestigationCase caseDomain = new InvestigationCase(
            caseId,
            reference,
            status,
            reportedBy,
            properties.getDefaultAssignee(),
            initialNote,
            now,
            now,
            now
        );

        InvestigationCaseEntity entity = InvestigationCaseEntity.fromDomain(caseDomain);
        entity = repository.save(entity);

        log.info("NAMNM INV case created caseId={} reference={} reportedBy={} note={} ",
            entity.getId(), reference, reportedBy, initialNote);

        PaymentStatusClient.PaymentStatusSnapshot snapshot = paymentStatusClient.fetchStatus(reference);
        if (snapshot.success()) {
            log.info("NAMNM INV payment-status reference={} status={} ", reference, snapshot.status());
        } else {
            log.warn("NAMNM INV payment-status-failed reference={} error={} ", reference, snapshot.errorMessage());
        }

        return entity.toDomain();
    }

    @Transactional(readOnly = true)
    public Optional<InvestigationCase> findById(UUID caseId) {
        return repository.findById(caseId).map(InvestigationCaseEntity::toDomain);
    }

    @Transactional(readOnly = true)
    public Optional<InvestigationCase> findByReference(String reference) {
        return repository.findByReference(reference).map(InvestigationCaseEntity::toDomain);
    }

    public Optional<InvestigationCase> updateStatus(UUID caseId, InvestigationStatus status, String note, String assignedTo) {
        return repository.findById(caseId).map(entity -> {
            entity.setStatus(status);
            entity.setStatusChangedAt(OffsetDateTime.now());
            if (note != null && !note.isBlank()) {
                entity.setLastNote(note);
            }
            if (assignedTo != null && !assignedTo.isBlank()) {
                entity.setAssignedTo(assignedTo);
            }
            log.info("NAMNM INV case-status-updated caseId={} status={} note={} assignedTo={} ",
                caseId, status, note, assignedTo);
            return entity.toDomain();
        });
    }

    public Optional<InvestigationCase> appendNote(UUID caseId, String note) {
        return repository.findById(caseId).map(entity -> {
            entity.setLastNote(note);
            log.info("NAMNM INV case-note-updated caseId={} note={} ", caseId, note);
            return entity.toDomain();
        });
    }

    @Transactional(readOnly = true)
    public Map<InvestigationStatus, Long> statusCount() {
        Map<InvestigationStatus, Long> result = new EnumMap<>(InvestigationStatus.class);
        for (InvestigationStatus status : InvestigationStatus.values()) {
            result.put(status, repository.countByStatus(status));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<InvestigationCase> recentByReference(String reference) {
        return repository.findTop20ByReferenceOrderByUpdatedAtDesc(reference)
            .stream()
            .map(InvestigationCaseEntity::toDomain)
            .toList();
    }
}
