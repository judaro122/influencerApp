# InfluencerAPP - Project Constitution

## 1. Purpose & Scope

InfluencerAPP is a multi-tenant Spring Boot REST service that automates YouTube video publishing. It accepts video uploads, generates AI-powered metadata via Google Gemini, uploads to user-linked YouTube channels, and emits lifecycle events through Apache Kafka.

This constitution defines the non-negotiable principles, architecture rules, and quality gates that govern all development decisions.

## 2. Governing Principles

1. **Hexagonal Architecture Strictly Enforced** — domain layer contains zero framework dependencies. All I/O (HTTP, DB, Kafka, storage, AI, YouTube) is accessed exclusively through ports and adapters.
2. **Multi-Tenancy by Default** — every query, every storage path, and every Kafka payload MUST include tenant isolation. Cross-tenant data leakage is a P0 violation.
3. **Streaming Over Buffering** — video files MUST NEVER be fully loaded into application memory. Uploads to MinIO/S3 and YouTube MUST use streaming/chunked transfer.
4. **Eventual Consistency via Events** — the upload pipeline is asynchronous by design. Synchronous waiting for YouTube publish is forbidden. All state transitions are communicated via Kafka events.
5. **Secrets Never in Code** — all credentials, API keys, and tokens are injected via environment variables or external secret stores. No hardcoded secrets. No secrets in version control.
6. **Free-Tier Constraints Are First-Class Requirements** — Gemini rate limits and YouTube quota limits are architectural constraints, not operational afterthoughts. Circuit breakers and fallbacks are mandatory.
7. **Clean Code & Maintainability** — code MUST be readable, testable, and SOLID-compliant. Classes and methods follow single responsibility. Names reveal intent. Cyclomatic complexity MUST remain low. No dead code, no commented-out blocks.
8. **Lombok for Boilerplate Elimination** — Lombok annotations (`@Data`, `@Builder`, `@Slf4j`, `@Value`, `@With`, `@RequiredArgsConstructor`) are the standard for reducing verbosity in domain, application, and infrastructure layers. Manual getters/setters/constructors are forbidden except where Lombok cannot express the invariant.
9. **Maven as Canonical Build** — the project uses Maven as the single build and dependency management tool. Multi-module structure is required. Versions are managed via parent POM. No Gradle or other build tools permitted.

## 3. Architecture Constraints

### 3.1 Layer Boundaries
| Layer | Allowed Dependencies | Forbidden |
|-------|---------------------|-----------|
| `domain` | Java 17+ standard library, `jakarta.validation` | Spring, JPA, Kafka, HTTP clients, AI SDKs |
| `application` | `domain`, `spring-context` | `spring-web`, `spring-data`, `spring-kafka` |
| `infrastructure` | `application`, `domain`, all external SDKs | None — this is the adapter layer |

### 3.2 Mandatory Adapters
- **Storage**: `MinIOStorageAdapter` required. `S3StorageAdapter` optional but MUST implement the same `ObjectStoragePort`.
- **AI**: `GeminiAdapter` is the canonical implementation. Alternative AI adapters MAY exist but MUST implement `AITextGenerationPort`.
- **YouTube**: `YouTubeUploadAdapter` MUST handle token refresh transparently before upload.

### 3.3 Database
- PostgreSQL is the only permitted persistence store.
- JPA entities MUST map 1:1 with domain models. No anemic domain models.
- OAuth2 tokens MUST be stored encrypted at rest using JCE.

### 3.4 Coding Standards
- **Lombok First**: All DTOs, entities, and value objects MUST use Lombok annotations to eliminate boilerplate. Allowed annotations: `@Data`, `@Builder`, `@Slf4j`, `@Value`, `@With`, `@RequiredArgsConstructor`, `@NoArgsConstructor(force = true)`. Manual getters, setters, equals, hashCode, toString, and constructors are forbidden unless Lombok cannot express the invariant (e.g., custom validation in constructor).
- **Naming**: Class names are nouns (`UploadVideoUseCase`). Method names are verbs (`execute`, `publish`, `validate`). Boolean methods prefix with `is`/`has`/`can`. Constants are `UPPER_SNAKE_CASE`.
- **Immutability**: Domain models and value objects MUST be immutable. Use `@Value` (Lombok) or `final` fields with `@RequiredArgsConstructor`.
- **No God Classes**: A class MUST NOT exceed 300 lines. A method MUST NOT exceed 30 lines. If exceeded, refactor immediately.
- **No Static State**: Static mutable fields are forbidden. Constants are permitted.
- **Exception Handling**: Use domain-specific exceptions (`DomainException`, `TenantIsolationViolationException`). Never catch generic `Exception` without rethrow or wrapping.
- **Logging**: Use `@Slf4j` (Lombok) for all classes requiring logging. No `System.out.println` or `System.err.println`.
- **Null Safety**: Avoid `null` references. Use `Optional` for nullable return values. Use `@NonNull` / `@Nullable` annotations where applicable.

## 4. Security & Compliance Rules

1. **Authentication**: All endpoints except `/api/auth/**` and `/api/health` require valid JWT Bearer tokens.
2. **Authorization**: Users MAY only access their own channels and videos. Channel ownership MUST be verified before any YouTube operation.
3. **Token Encryption**: `access_token`, `refresh_token`, and `token_expiry` in the `channels` table MUST be encrypted using AES/GCM/NoPadding with a key from `${JWT_SECRET}` or a dedicated `${ENCRYPTION_KEY}`.
4. **YouTube Scopes**: Only `https://www.googleapis.com/auth/youtube.upload` is requested. No additional scopes without explicit approval.
5. **Input Validation**: All multipart uploads MUST be validated for file type, size, and metadata. Max file size is 100MB by default, configurable via `spring.servlet.multipart.max-file-size`.
6. **HTTPS Only**: In production, all external communications MUST use TLS. Local HTTP is permitted only for `localhost` development.

## 5. API Contract Rules

1. **RESTful Design**: All endpoints use standard HTTP verbs. Paths use plural nouns (`/api/videos`, `/api/channels`).
2. **Error Responses**: All errors return RFC 7807 `application/problem+json` format with `type`, `title`, `status`, `detail`, and `instance`.
3. **Idempotency**: `POST /api/videos/upload` MUST support `Idempotency-Key` header to prevent duplicate uploads on retry.
4. **Pagination**: All list endpoints MUST return paginated results with `page`, `size`, and `totalElements`.

## 6. Kafka Event Rules

1. **Schema Registry**: All event payloads MUST be versioned. Topic names use kebab-case (`video-received`, `video-published`).
2. **Eventual Consistency**: Consumers MUST be idempotent. Duplicate event delivery MUST NOT cause duplicate YouTube uploads.
3. **Dead Letter Queue**: Events that fail after 3 retries MUST be routed to a DLQ for manual inspection.
4. **Payload Size**: Event payloads MUST NOT contain binary data. Video file references use storage paths/URIs only.

## 7. Failure Handling & Resilience

1. **Retry Policy**: External calls (Gemini, YouTube, MinIO, Kafka) use exponential backoff with jitter: initial 1s, max 30s, 3 attempts.
2. **Circuit Breakers**: YouTube and Gemini integrations MUST use Resilience4j circuit breakers. Failure threshold: 50% over 10s, wait 60s before retry.
3. **Graceful Degradation**: If Gemini fails, the system MUST fall back to placeholder text: `"Title for {filename}"` and `"Description for {filename}"`. Upload proceeds.
4. **Storage Fallback**: If MinIO is unreachable, the system MAY fall back to local filesystem ONLY if `storage.type=local` is explicitly configured. Default is `minio`.
5. **Kafka Buffering**: If Kafka is unavailable, events MUST be persisted to a local buffer and retried on reconnect. In-memory buffer is acceptable for development; production MUST use persistent outbox pattern.

## 8. Testing Standards

1. **Clean Code in Tests**: Test classes follow the same Clean Code principles as production code. Test method names describe the scenario and expected outcome. No test logic duplication. Use `@DisplayName` for readability.
2. **Lombok in Tests**: Lombok annotations are permitted in test classes (`@Slf4j`, `@Data` for DTOs, `@RequiredArgsConstructor` for constructor injection in tests).
3. **Unit Tests**: All use cases (`*UseCase.java`) MUST have unit tests with mocked ports. Target coverage: 90%+ for domain and application layers.
4. **Integration Tests**: All adapters MUST have integration tests using Testcontainers (PostgreSQL, Kafka, MinIO).
5. **Contract Tests**: REST controllers MUST have contract tests validating request/response schemas.
6. **No External Calls in Unit Tests**: All external SDKs (Gemini, YouTube, Kafka) MUST be mocked. Integration tests are the ONLY place real external calls are permitted.

## 9. Observability Requirements

1. **Structured Logging**: All logs use JSON format with correlation IDs. Correlation ID MUST flow from HTTP request through Kafka events to downstream consumers.
2. **Metrics**: All adapters MUST expose latency and error counters. Key metrics: `upload.duration`, `gemini.latency`, `youtube.quota.used`, `kafka.publish.latency`.
3. **Health Checks**: `/api/health` MUST report status of PostgreSQL, Kafka, MinIO, and YouTube API connectivity.

## 10. Deployment Constraints

1. **Docker First**: All services are containerized. `docker-compose.yml` MUST define app, postgres, kafka, zookeeper, and minio.
2. **Configuration**: All secrets via environment variables or Docker secrets. No `.env` files committed to version control.
3. **Database Migrations**: Flyway or Liquibase REQUIRED. No manual schema changes. Migrations run on application startup.

## 11. Out of Scope (Non-Negotiable Boundaries)

The following are explicitly excluded and MUST NOT be implemented in v1.0:
1. Video transcoding or format conversion
2. Thumbnail generation or image processing
3. Scheduling or delayed publishing
4. WebSocket or Server-Sent Events for real-time client notifications
5. Admin dashboard or analytics UI
6. Batch/multi-video upload in a single request
7. Multi-language metadata generation

## 12. Amendment Process

This constitution may be amended only by:
1. Explicit written proposal documenting the change, rationale, and impact
2. Review and approval by project maintainers
3. Version bump of this document with changelog entry

---

*Version: 1.0.0*  
*Status: Ratified*  
*Last Updated: 2026-07-28*
