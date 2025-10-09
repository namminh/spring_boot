package com.corebank.investigation.api;

import com.corebank.investigation.api.dto.CreateInvestigationRequest;
import com.corebank.investigation.api.dto.InvestigationResponse;
import com.corebank.investigation.api.dto.UpdateInvestigationStatusRequest;
import com.corebank.investigation.application.InvestigationService;
import com.corebank.investigation.domain.InvestigationCase;
import com.corebank.investigation.domain.InvestigationStatus;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/investigations")
public class InvestigationController {

    private static final Logger log = LoggerFactory.getLogger(InvestigationController.class);

    private final InvestigationService service;

    public InvestigationController(InvestigationService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<InvestigationResponse> create(@Valid @RequestBody CreateInvestigationRequest request) {
        log.info("NAMNM INV create requested reference={} reportedBy={}", request.reference(), request.reportedBy());
        InvestigationCase created = service.createCase(request.reference(), request.reportedBy(), request.initialNote());
        InvestigationResponse response = InvestigationResponse.fromDomain(created);
        return ResponseEntity.created(URI.create("/api/v1/investigations/" + created.getId()))
            .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<InvestigationResponse> getById(@PathVariable UUID id) {
        log.info("NAMNM INV find-by-id caseId={}", id);
        return service.findById(id)
            .map(InvestigationResponse::fromDomain)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/reference/{reference}")
    public ResponseEntity<List<InvestigationResponse>> getByReference(@PathVariable String reference) {
        log.info("NAMNM INV find-by-reference reference={}", reference);
        List<InvestigationResponse> cases = service.recentByReference(reference)
            .stream()
            .map(InvestigationResponse::fromDomain)
            .toList();
        return ResponseEntity.ok(cases);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<InvestigationResponse> updateStatus(@PathVariable UUID id,
                                                              @Valid @RequestBody UpdateInvestigationStatusRequest request) {
        log.info("NAMNM INV update-status caseId={} status={}", id, request.status());
        return service.updateStatus(id, request.status(), request.note(), request.assignedTo())
            .map(InvestigationResponse::fromDomain)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/note")
    public ResponseEntity<InvestigationResponse> appendNote(@PathVariable UUID id,
                                                            @RequestBody Map<String, String> payload) {
        String note = payload.get("note");
        log.info("NAMNM INV append-note caseId={}", id);
        return service.appendNote(id, note)
            .map(InvestigationResponse::fromDomain)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/metrics/status-count")
    public Map<InvestigationStatus, Long> statusCount() {
        log.info("NAMNM INV status-count requested");
        return service.statusCount();
    }
}
