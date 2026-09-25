# InfluencerAPP — Test Specification

## 1. Overview

This document defines the testing strategy, Testcontainers configuration, and code coverage requirements for the InfluencerAPP platform. It is derived from the [Project Constitution](../constitution.md), the [Architecture Specification](../architecture.md), the [Data Model Specification](arq/data-model.md), and the [Integrations Specification](integrations-spec.md).

---

## 2. Testing Standards (Constitution §8)

### 2.1 Clean Code in Tests

- Test classes follow the same Clean Code principles as production code.
- Test method names describe the scenario and expected outcome.
- No test logic duplication; use helper methods and builders.
- Use `@DisplayName` for readable test names.

### 2.2 Lombok in Tests

Lombok annotations are permitted in test classes:

| Annotation | Usage |
|------------|-------|
| `@Slf4j` | Logging in test classes |
| `@Data` | DTOs used in test data builders |
| `@RequiredArgsConstructor` | Constructor injection in test classes |

### 2.3 Unit Tests

- All use cases (`*UseCase.java`) MUST have unit tests with mocked ports.
- Target coverage: **90%+ for domain and application layers**.
- External SDKs (Gemini, YouTube, Kafka, MinIO, Spring Web) MUST be mocked.
- No database, no Kafka, no HTTP calls in unit tests.

### 2.4 Integration Tests

- All adapters MUST have integration tests using Testcontainers.
- Full Spring context with Testcontainers PostgreSQL, Kafka, and MinIO.
- Repository tests (`JpaVideoRepositoryIT`, `JpaChannelRepositoryIT`) use real PostgreSQL via Testcontainers.

### 2.5 Contract Tests

- REST controllers MUST have contract tests validating request/response schemas.
- Use SpringDoc OpenAPI 3.0 (SpringDoc) and Spring REST Docs or `restdocs-api-spec` for contract validation.
- Validate request/response structure, status codes, and headers (`Idempotency-Key`).

### 2.6 Out of Scope in Tests

- No external calls (Gemini, YouTube, Kafka) in unit tests.
- No real YouTube API in integration tests; use WireMock or recorded stubs.

---

## 3. Testcontainers Configuration

### 3.1 Dependencies

Add the following to `pom.xml` (test scope):

```xml
<dependency>
  <groupId>org.testcontainers</groupId>
  <artifactId>testcontainers</artifactId>
  <version>1.19.0</version>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>org.testcontainers</groupId>
  <artifactId>junit-jupiter</artifactId>
  <version>1.19.0</version>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>org.testcontainers</groupId>
  <artifactId>postgresql</artifactId>
  <version>1.19.0</version>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>org.testcontainers</groupId>
  <artifactId>kafka</artifactId>
  <version>1.19.0</version>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>org.testcontainers</groupId>
  <artifactId>minio</artifactId>
  <version>1.19.0</version>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>org.testcontainers</groupId>
  <artifactId>spring-boot</artifactId>
  <version>1.19.0</version>
  <scope>test</scope>
</dependency>
```

### 3.2 PostgreSQL Container

| Property | Value |
|----------|-------|
| Image | `postgres:15` |
| Database | `influencerapp_test` |
| Username | `test` |
| Password | `test` |
| Port | `5432` (auto-assigned) |
| Init script | `db/migration/V1__init.sql` from `src/main/resources/db/migration/` |
| Wait strategy | Wait for port 5432 to be accepting connections |

**JUnit 5 integration:**

```java
@Container
static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
    .withDatabaseName("influencerapp_test")
    .withUsername("test")
    .withPassword("test")
    .withInitScript("db/migration/V1__init.sql");
```

**Spring Boot test property override:**

| Property | Value |
|----------|-------|
| `spring.datasource.url` | `postgres.getJdbcUrl()` |
| `spring.datasource.username` | `test` |
| `spring.datasource.password` | `test` |
| `spring.flyway.enabled` | `true` |

### 3.3 Kafka Container

| Property | Value |
|----------|-------|
| Image | `confluentinc/cp-kafka:7.5.0` |
| Requires | `confluentinc/cp-zookeeper:7.5.0` (embedded with Kafka container) |
| Port | `9092` (auto-assigned) |
| Environment | `KAFKA_AUTO_CREATE_TOPICS_ENABLE=true` |
| Topics | `video-received`, `video-published`, `video-received-dlq`, `video-published-dlq` |
| Wait strategy | Wait for container to be healthy and topic creation to succeed |

**JUnit 5 integration:**

```java
@Container
static KafkaContainer kafka = new KafkaContainer("confluentinc/cp-kafka:7.5.0")
    .withExposedPorts(9092)
    .withReuse(true);
```

**Spring Boot test property override:**

| Property | Value |
|----------|-------|
| `spring.kafka.bootstrap-servers` | `kafka.getBootstrapServers()` |
| `spring.kafka.consumer.auto-offset-reset` | `earliest` |
| `spring.kafka.producer.key-serializer` | `org.apache.kafka.common.serialization.StringSerializer` |
| `spring.kafka.producer.value-serializer` | `org.springframework.kafka.support.serializer.JsonSerializer` |

### 3.4 MinIO Container

| Property | Value |
|----------|-------|
| Image | `minio/minio:latest` |
| Port | `9000` (API), `9001` (Console) |
| Environment | `MINIO_ROOT_USER=test`, `MINIO_ROOT_PASSWORD=test` |
| Command | `server /data --console-address ":9001"` |
| Wait strategy | Wait for port 9000 to be accepting connections |
| Bucket | `video-bucket` (created via SDK in test setup) |

**JUnit 5 integration:**

```java
@Container
static MinIOContainer minio = new MinIOContainer("minio/minio:latest")
    .withExposedPorts(9000, 9001)
    .withReuse(true)
    .withEnv("MINIO_ROOT_USER", "test")
    .withEnv("MINIO_ROOT_PASSWORD", "test");
```

**Spring Boot test property override:**

| Property | Value |
|----------|-------|
| `spring.minio.endpoint` | `minio.getEndpoint()` |
| `spring.minio.access-key` | `test` |
| `spring.minio.secret-key` | `test` |
| `spring.minio.bucket` | `video-bucket` |

### 3.5 Full Testcontainers Composition (JUnit 5)

All containers run as static fields in a shared `@Container` outer class, ensuring they start once and are reused across all tests in the test class:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
abstract class IntegrationTestBase {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("influencerapp_test")
        .withUsername("test")
        .withPassword("test")
        .withInitScript("db/migration/V1__init.sql");

    @Container
    static KafkaContainer kafka = new KafkaContainer("confluentinc/cp-kafka:7.5.0")
        .withExposedPorts(9092)
        .withReuse(true)
        .withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "true");

    @Container
    static MinIOContainer minio = new MinIOContainer("minio/minio:latest")
        .withExposedPorts(9000, 9001)
        .withReuse(true)
        .withEnv("MINIO_ROOT_USER", "test")
        .withEnv("MINIO_ROOT_PASSWORD", "test");

    static {
        postgres.start();
        kafka.start();
        minio.start();
    }

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.minio.endpoint", minio::getEndpoint);
        registry.add("spring.minio.access-key", () -> "test");
        registry.add("spring.minio.secret-key", () -> "test");
        registry.add("spring.minio.bucket", () -> "video-bucket");
    }
}
```

### 3.6 Container Lifecycle

| Phase | Action |
|-------|--------|
| Test class setup | All containers start (static `@Container` fields) |
| Per test | Spring context loads with overridden properties |
| Per test teardown | No container stop; containers persist for reuse across tests |
| Suite teardown | Containers stop after all tests complete |

Use `--reuse` flag (MinIO, Kafka) to avoid recreating containers on repeated test runs.

---

## 4. Code Coverage Requirements

### 4.1 Target Coverage

| Layer | Target | Mandatory? |
|-------|--------|------------|
| `domain` | 90%+ | Yes |
| `application` | 90%+ | Yes |
| `infrastructure` | No minimum | No (adapters have integration tests instead) |

### 4.2 What Is Covered

| Layer | Elements Included in Coverage |
|-------|-------------------------------|
| `domain` | All aggregate roots (`Video`, `Channel`), value objects (`TenantId`, `VideoId`, `ChannelId`, `FileMetadata`, etc.), domain services, ports (interfaces), and domain exceptions |
| `application` | All use cases (`UploadVideoUseCase`, `RegisterChannelUseCase`, `GetVideoStatusUseCase`), application services (`UploadVideoService`, `MetadataGenerationService`, etc.), application events (`VideoReceivedEvent`, `VideoPublishedEvent`), and DTOs |

### 4.3 What Is NOT Included in Coverage Calculation

| Element | Reason |
|---------|--------|
| `infrastructure` layer (adapters, repositories, entities) | Covered by integration tests with Testcontainers, not unit test metrics |
| Configuration classes (`KafkaConfig`, `SecurityConfig`, `StorageConfig`) | Infrastructure wiring; not domain or application logic |
| REST controllers | Covered by contract tests (§2.5) |
| DTOs and request/response objects | Trivial getters/setters; not tested independently |
| Exception mappers | Not part of domain or application logic |

### 4.4 Coverage Tooling

| Tool | Purpose |
|------|---------|
| JaCoCo | Code coverage measurement via Maven (`mvn verify`) |
| Maven Surefire | Unit test execution |
| Maven Failsafe | Integration test execution (Testcontainers) |
| SonarQube | Coverage reporting and quality gate (90% for domain + application) |

### 4.5 Coverage Enforcement (CI)

The build MUST fail if domain or application layer coverage drops below 90%:

```xml
<plugin>
  <groupId>org.jacoco</groupId>
  <artifactId>jacoco-maven-plugin</artifactId>
  <configuration>
    <rules>
      <rule>
        <element>BUNDLE</element>
        <limits>
          <limit>
            <counter>LINE</counter>
            <value>COVEREDRATIO</value>
            <minimum>0.90</minimum>
          </limit>
        </limits>
        <excludes>
          <exclude>com.influencerapp.infrastructure.*</exclude>
          <exclude>com.influencerapp.config.*</exclude>
          <exclude>com.influencerapp.controller.*</exclude>
        </excludes>
      </rule>
    </rules>
  </configuration>
</plugin>
```

---

## 5. Test Categories

### 5.1 Unit Tests

| Category | Location | Description |
|----------|----------|-------------|
| Use case tests | `domain/**/*UseCaseTest.java` | Mocked ports; verify business logic |
| Domain model tests | `domain/**/*Test.java` | Value object invariants, aggregate rules |
| Port tests | `domain/**/*PortTest.java` | Interface contract validation |
| Service tests | `application/**/*ServiceTest.java` | Mocked ports; orchestration logic |
| DTO tests | `application/**/*DtoTest.java` | Serialization, deserialization, validation |
| Event tests | `application/**/*EventTest.java` | Event construction, field correctness |

### 5.2 Integration Tests (Testcontainers)

| Category | Class Suffix | Description |
|----------|-------------|-------------|
| PostgreSQL repository | `JpaVideoRepositoryIT`, `JpaChannelRepositoryIT` | Real DB operations, Flyway migrations |
| MinIO storage | `MinIOStorageAdapterIT` | Upload/download/delete with real MinIO |
| Kafka producer/consumer | `VideoEventProducerIT`, `VideoEventConsumerIT` | Produce and consume real Kafka events |
| Full Spring context | `*IntegrationIT` | Full application context with all Testcontainers |
| Adapter tests | `*AdapterIT` | All adapters with real external dependencies |

### 5.3 Contract Tests

| Category | Description |
|----------|-------------|
| REST controller contracts | Validate request/response schemas against OpenAPI 3.0 |
| RFC 7807 error contracts | Validate error response structure (type, title, status, detail, instance) |
| Pagination contract | Validate paginated response envelope |
| Idempotency contract | Validate `Idempotency-Key` header behavior |

---

## 6. Test Data Management

### 6.1 Flyway Test Migrations

- Test migrations are stored in `src/test/resources/db/migration/`.
- The same migration scripts as production are applied via Flyway on context startup.
- Test-specific data (seed data) is loaded via `@Sql` annotations or testcontainers init scripts.

### 6.2 Data Isolation

- Each test suite uses a dedicated test database (`influencerapp_test`).
- Test data is cleaned up between test classes using `@DirtiesContext` or `@Sql(summary = "cleanup")`.
- Tenant isolation is verified by testing multi-tenant scenarios explicitly (same video ID for different tenants must not conflict).

---

## 7. Summary

| Concern | Configuration |
|---------|---------------|
| PostgreSQL | `postgres:15`, auto-created DB, Flyway migrations |
| Kafka | `confluentinc/cp-kafka:7.5.0`, auto-topic creation |
| MinIO | `minio/minio:latest`, bucket created via SDK |
| Unit test coverage | 90%+ for `domain` and `application` layers |
| Integration tests | Testcontainers for all adapters and repositories |
| Contract tests | OpenAPI 3.0 schema validation via SpringDoc + Spring REST Docs |
| Coverage enforcement | JaCoCo configured in `pom.xml`; CI fails below 90% |

---

## 8. Automated API Testing for Deployed Services

For end-to-end black-box and integration testing against deployed environments (e.g. `http://192.168.100.10:8080`), a standalone Java test automation suite based on REST Assured, JUnit 5, and Allure is specified.

Detailed specification: [Automated API Test Plan](automated-test-plan.md)

### Key Test Categories:
- **Smoke Tests**: System health (`GET /api/health`), component availability.
- **Authentication & Security**: Registration, JWT login, token expiration, multi-tenant isolation.
- **Channel Operations**: OAuth token storage, encrypted credentials, channel pagination.
- **Video Upload & Idempotency**: Multipart uploads, duplicate `Idempotency-Key` caching, invalid MIME rejection.
- **E2E Publishing Lifecycle**: Async polling (`RECEIVED` -> `PROCESSING` -> `PUBLISHED`), Gemini metadata fallback.
- **Contract & RFC 7807 Compliance**: OpenAPI schema compliance and standardized error payloads.

---

*Version: 1.1.0*  
*Status: Active*  
*Last Updated: 2026-09-25*