package com.corebank.orchestrator.adapter;

import com.corebank.orchestrator.domain.Alert;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    List<Alert> findByAcknowledgedFalseOrderByCreatedAtDesc();

    long countByAcknowledgedFalse();
}
