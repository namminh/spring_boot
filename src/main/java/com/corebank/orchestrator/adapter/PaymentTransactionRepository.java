package com.corebank.orchestrator.adapter;

import com.corebank.orchestrator.domain.PaymentTransaction;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    List<PaymentTransaction> findTop20ByProcessedAtAfterOrderByProcessedAtDesc(OffsetDateTime processedAfter);

    long countByStatus(String status);
}
