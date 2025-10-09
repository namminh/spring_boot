package com.corebank.investigation.infrastructure.persistence;

import com.corebank.investigation.domain.InvestigationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvestigationCaseRepository extends JpaRepository<InvestigationCaseEntity, UUID> {

    Optional<InvestigationCaseEntity> findByReference(String reference);

    long countByStatus(InvestigationStatus status);

    List<InvestigationCaseEntity> findTop20ByReferenceOrderByUpdatedAtDesc(String reference);
}
