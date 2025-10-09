package com.corebank.payment.infrastructure.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxRepository extends JpaRepository<OutboxEventEntity, Long> {

    List<OutboxEventEntity> findTop20ByStatusOrderByCreatedAtAsc(OutboxStatus status);
}
