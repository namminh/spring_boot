# Java & Spring Core Concepts Study Guide / Ghi chú luyện Java – Spring

Tài liệu này tóm tắt các khái niệm trọng yếu xuất hiện trong dự án CoreBank Payment Lab (Java 21, Spring Boot 3.2) để bạn ôn tập trước phỏng vấn. Mỗi mục gồm giải thích, lifecycle, ví dụ code, và liên hệ dự án. Song ngữ Anh/Việt (EN/VI) giúp luyện phản xạ.

---

## 1. Spring Bean & ApplicationContext

- **Definition (EN)**: A Spring Bean is an object managed by the Spring IoC container (ApplicationContext). Lifecycle includes instantiation, dependency injection, post-processing, initialization, usage, destruction.
- **Định nghĩa (VI)**: Bean là đối tượng được ApplicationContext tạo và quản lý. Vòng đời gồm khởi tạo, inject phụ thuộc, post-process, init, sử dụng và hủy.
- **Lifecycle hooks**: `@PostConstruct`, `InitializingBean.afterPropertiesSet()`, `@PreDestroy`, `DisposableBean.destroy()`, plus custom `BeanPostProcessor`.
- **Project link**: `PaymentOrchestratorService` (`@Service`), `CacheConfig` (`@Configuration`, `@Bean cacheManager`), `OutboxRelay` (`@Component` + `@Scheduled`).

```java
@Configuration
public class CacheConfig {
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        return RedisCacheManager.builder(factory).build();
    }
}
```

- **Checklist**:
  - Understand scope (`singleton`, `prototype`, `request`, etc.).
  - Explain how Spring Boot auto-configuration registers beans (`@ConfigurationProperties`, conditional beans).

---

## 2. Dependency Injection (DI) & Inversion of Control (IoC)

- **EN**: DI lets Spring inject dependencies via constructor, setter, or field. Preferred style is constructor injection for immutability/testability.
- **VI**: DI cho phép Spring tiêm phụ thuộc qua constructor/setter/field. Ưu tiên constructor để đảm bảo bất biến và dễ test.
- **Project link**: `PaymentController` constructor-injects `PaymentOrchestratorService`; `TuxedoGateway` injects `TuxedoClient`.
- **Interview point**: be ready to contrast DI with manual `new`, and explain how IoC container decides which bean implementation to inject (qualifiers, primary).

---

## 3. Bean Scopes & Proxying

- **Scopes**:
  - `singleton` (default) – one instance per ApplicationContext.
  - `prototype` – new instance per injection request.
  - Web scopes: `request`, `session`.
- **Proxying**: `@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)` for injecting scoped beans into singletons.
- **Project usage**: Payment services use default singleton scope; `RestTemplate` bean is singleton but thread-safe.

---

## 4. Spring Configuration & Profiles

- **Profiles**: `@Profile("lab-java21")` toggles beans/config for specific environments.
- **Property binding**: `@ConfigurationProperties(prefix = "investigation")` binds YAML to POJO (`InvestigationProperties`).
- **Project link**: `spring.threads.virtual.enabled=true` in `application-lab-java21.yml`; `PaymentStatusClient` properties under different profiles.
- **Lifecycle**: properties loaded ➜ environment prepared ➜ beans created with profile conditions.

---

## 5. Scheduling & Async Processing

- **Scheduler**: `@EnableScheduling` on main application, `@Scheduled(fixedDelayString = "...")` to run tasks (e.g., `OutboxRelay.forwardPendingEvents`).
- **Async**: `@Async` + `TaskExecutor`; in project we use virtual threads via structured concurrency instead of `@Async` for certain tasks.
- **Interview note**: Understand difference between `fixedDelay`, `fixedRate`, `cron`; thread pool tuning.

---

## 6. Transaction Management

- **Definition**: `@Transactional` ensures ACID boundaries. Works with proxies intercepting method calls.
- **Project usage**: `PaymentOrchestratorService.orchestrate` uses transactional repositories; `OutboxRelay` often wraps DB updates in transactions to mark events dispatched.
- **Key points**:
  - Propagation (`REQUIRED`, `REQUIRES_NEW`).
  - Isolation (`READ_COMMITTED` etc.).
  - Rollback rules (checked vs unchecked exception).

---

## 7. Persistence with Spring Data JPA

- **Repositories**: `PaymentRepository extends JpaRepository<PaymentEntity, Long>`.
- **Entity lifecycle**: transient ➜ managed ➜ detached ➜ removed.
- **Auditing**: `@EntityListeners` for timestamps.
- **Project example**: `PaymentEntity`, `InvestigationCaseEntity`.
- **Interview**: talk about `EntityManager`, `flush`, `dirty checking`, pagination, projection.

---

## 8. Bean Validation (Jakarta Validation)

- **Annotations**: `@NotBlank`, `@Positive`, `@Valid`, `@Size`.
- **Controller**: `PaymentController.initiatePayment(@Valid PaymentRequestDto dto)`.
- **Explain**: how validation happens before method execution, customizing messages, and creating custom validators.

---

## 9. Resilience Patterns

- **Resilience4j**: `@Retry(name = "tuxedo-process")`, fallback methods, circuit breakers.
- **Timeouts**: configure via properties (`resilience4j.retry.instances`).
- **Project link**: `TuxedoGateway.process` with retry; `MockTuxedoClient` handles fallback scenarios.
- **Interview**: difference between retry, circuit breaker, bulkhead; how to monitor metrics.

---

## 10. Caching with Spring Cache & Redis

- **Annotations**: `@Cacheable`, `@CacheEvict`, `@CachePut`.
- **Project**: `PaymentOrchestratorService.findByReference` caches payments; `orchestrate` evicts after update.
- **Explain**: key generation, `unless` condition, TTL configuration (`CacheManager`).
- **Pitfall**: caching `Optional` vs null, synchronizing between replicas.

---

## 11. Virtual Threads & Structured Concurrency (Java 21)

- **Virtual Threads**: lightweight threads managed by JVM; enable high concurrency for IO-bound tasks.
- **StructuredTaskScope**: coordinate child virtual threads; `ShutdownOnFailure` ensures fail-fast.
- **Project**: `PaymentController.statusBreakdown`, `OutboxRelay.dispatchInParallel`.
- **Interview**: difference vs platform threads, when not to use (CPU-bound, long-lived locks).

```java
try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
    Map<PaymentStatus, Future<Long>> futures = new EnumMap<>(PaymentStatus.class);
    for (PaymentStatus status : PaymentStatus.values()) {
        futures.put(status, scope.fork(() -> orchestrator.countByStatus(status)));
    }
    scope.join().throwIfFailed();
    return futures.entrySet().stream()
            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, e -> e.getValue().resultNow()));
}
```

---

## 12. Spring Events & Messaging

- **ApplicationEventPublisher**: `SpringMonitoringEventPublisher` emits `AlertCreatedEvent`.
- **Outbox Pattern**: domain writes to outbox table ➜ relay publishes to Kafka.
- **Interview**: explain eventual consistency, deduplication, handling failures.

---

## 13. Testing Strategy

- **Unit tests**: use JUnit 5, Mockito. Focus on service logic, mapping.
- **Integration tests**: `@SpringBootTest`, `@DataJpaTest` with Testcontainers if DB required.
- **Contract tests**: use MockMvc for controller endpoints.
- **Project**: emphasise verifying orchestrator, outbox dispatch, Kafka transformer.

---

## 14. Quick Reference Q&A

1. **Q**: What is the lifecycle of a Spring Bean?  
   **A**: Instantiate ➜ Dependency injection ➜ `BeanPostProcessor` ➜ `@PostConstruct`/`InitializingBean` ➜ Usage ➜ `@PreDestroy`/`DisposableBean`.

2. **Q**: How does `@Bean` differ from `@Component`?  
   **A**: `@Bean` is used inside `@Configuration` classes to declare beans (usually third-party or custom construction). `@Component` annotates the class itself for component scanning. Both result in managed beans but differ in registration style.

3. **Q**: Why prefer constructor injection?  
   **A**: Ensures required dependencies are available, enables immutability, easier testing (no reflection), works well with `final` fields.

4. **Q**: How do you handle transaction rollback for checked exceptions?  
   **A**: Specify `@Transactional(rollbackFor = SomeCheckedException.class)` or convert to unchecked.

5. **Q**: When would you avoid virtual threads?  
   **A**: CPU-bound workloads, synchronised blocks holding locks long, or libraries not virtual-thread-friendly (native blocking without carrier release).

---

## 15. Study Routine

- Review one section per day; re-implement small snippets in a scratch project.
- Practice explaining each concept in 60 seconds (EN) then Vietnamese.
- Map concepts back to metrics: caching → SLA, transactions → consistency, virtual threads → throughput.

Keep this guide together with `docs/corebank_payment_lab_java21_detailed.md` for deeper code references.
