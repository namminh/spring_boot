package com.corebank.payment.api;

import com.corebank.payment.api.dto.PaymentRequestDto;
import com.corebank.payment.api.dto.PaymentResponseDto;
import com.corebank.payment.application.PaymentOrchestratorService;
import com.corebank.payment.domain.Payment;
import com.corebank.payment.domain.PaymentCommand;
import com.corebank.payment.domain.PaymentStatus;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.StructuredTaskScope;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentOrchestratorService orchestratorService;
    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    public PaymentController(PaymentOrchestratorService orchestratorService) {
        this.orchestratorService = orchestratorService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponseDto> initiatePayment(@Valid @RequestBody PaymentRequestDto request) {
        log.info("NAMNM REST receive reference={} channel={} amount={} currency={}",
                request.reference(), request.channel(), request.amount(), request.currency());
        Payment payment = orchestratorService.orchestrate(new PaymentCommand(
                request.reference(),
                request.amount(),
                request.currency(),
                request.channel(),
                request.debtorAccount(),
                request.creditorAccount()));

        PaymentResponseDto response = new PaymentResponseDto(payment.id(), payment.reference(), payment.status());
        log.info("NAMNM REST processed reference={} status={} paymentId={}",
                response.reference(), response.status(), response.paymentId());
        return ResponseEntity.created(URI.create("/api/v1/payments/" + payment.reference()))
                .body(response);
    }

    @GetMapping("/{reference}")
    public ResponseEntity<PaymentResponseDto> fetchPayment(@PathVariable String reference) {
        log.info("NAMNM REST lookup reference={}", reference);
        return orchestratorService.findByReference(reference)
                .map(payment -> {
                    PaymentResponseDto response = new PaymentResponseDto(payment.id(),
                            payment.reference(),
                            payment.status());
                    log.info("NAMNM REST found reference={} status={} paymentId={}",
                            response.reference(), response.status(), response.paymentId());
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {
                    log.info("NAMNM REST not-found reference={}", reference);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
                });
    }

    @GetMapping("/metrics/status-count")
    public Map<PaymentStatus, Long> statusBreakdown() {
        log.info("NAMNM REST status-metrics requested");
        try (var scope = new StructuredTaskScope.ShutdownOnFailure(Thread.ofVirtual().factory())) {
            Map<PaymentStatus, StructuredTaskScope.Subtask<Long>> subtasks = new EnumMap<>(PaymentStatus.class);
            for (PaymentStatus status : PaymentStatus.values()) {
                subtasks.put(status, scope.fork(() -> orchestratorService.countByStatus(status)));
            }

            scope.join();
            scope.throwIfFailed();

            Map<PaymentStatus, Long> stats = new EnumMap<>(PaymentStatus.class);
            subtasks.forEach((status, subtask) -> {
                try {
                    stats.put(status, subtask.get());
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Status metrics aggregation interrupted", interruptedException);
                } catch (ExecutionException executionException) {
                    throw new IllegalStateException("Status metrics aggregation failed", executionException.getCause());
                }
            });
            log.info("NAMNM REST status-metrics result={} ", stats);
            return stats;
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Status metrics aggregation interrupted", ie);
        }
    }
}
