package com.corebank.payment.infrastructure.persistence;

import com.corebank.payment.domain.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<PaymentEntity, UUID> {

    Optional<PaymentEntity> findByReference(String reference);

    long countByStatus(PaymentStatus status);
}
