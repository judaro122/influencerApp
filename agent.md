# Agent Guide — InfluencerAPP

This file gives AI coding agents the mandatory rules for contributing to this repository. If any instruction here conflicts with a user request, the stricter rule wins. When in doubt, refuse and ask for clarification.

## 1. Governing Documents (Read First)

Before writing or modifying any code, read these files in full:

1. `docs/specs/constitution.md` — non-negotiable principles, architecture constraints, security rules, testing standards.
2. `arq/architecture.md` — derived architecture decisions, layer boundaries, package structure, API contracts, Kafka design, resilience, observability.
3. `docs/specs/PRD.md` — product requirements, features, success metrics, project structure.

You MUST NOT propose or implement anything that violates the Constitution. If a feature request conflicts with §11 (Out of Scope), reject it and cite the specific section.

## 2. Architectural Constraints

### 2.1 Hexagonal Architecture (Strict)
- **domain**: zero framework dependencies. No Spring, JPA, Kafka, HTTP clients, AI SDKs, or persistence frameworks.
- **application**: may use `spring-context` and `spring-boot` configuration only. No `spring-web`, `spring-data`, `spring-kafka`, or external SDKs.
- **infrastructure**: adapters, config, repositories, entities. All external SDKs live here.

Enforcement: Maven multi-module. `domain` must compile without `application` or `infrastructure` on the classpath.

### 2.2 Multi-Tenancy (Non-Negotiable)
Every query, storage path, and Kafka payload MUST include tenant isolation. Cross-tenant data leakage is a P0 violation.
- TenantId flows from JWT `sub` claim through all use cases.
- All DB queries include `WHERE tenant_id = :tenantId`.
- Storage paths are prefixed: `tenants/{tenantId}/...`.
- Kafka event payloads include `tenantId` at top level.

### 2.3 Streaming Over Buffering
Video files MUST NEVER be fully loaded into application memory (`byte[]`). All uploads/downloads use `InputStream` or `java.nio.channels.ReadableByteChannel`. This applies to MinIO, local filesystem, and YouTube upload adapters.

### 2.4 Eventual Consistency via Events
The upload pipeline is asynchronous by design. No synchronous HTTP wait for YouTube publish. All state transitions are communicated via Kafka events.

### 2.5 Secrets Never in Code
All credentials, API keys, and tokens are injected via environment variables. No hardcoded secrets. No secrets in version control.

## 3. Clean Code & Lombok Rules

### 3.1 Lombok First
Lombok is the canonical tool for eliminating Java boilerplate. Use these annotations:

| Annotation | Usage |
|------------|-------|
| `@Value` | Immutable domain models and value objects |
| `@Data` | Mutable DTOs and request/response objects |
| `@Builder` | Complex object construction |
| `@Slf4j` | Logging in any class that logs |
| `@RequiredArgsConstructor` | Constructor injection in services and adapters |
| `@With` | Non-destructive mutation for value objects |
| `@NoArgsConstructor(force = true)` | JPA entities only (Hibernate requirement) |

Manual getters, setters, equals, hashCode, toString, and constructors are FORBIDDEN unless Lombok cannot express the invariant (e.g., custom validation in constructor).

### 3.2 Clean Code Standards
- **Single Responsibility**: one class = one reason to change.
- **Meaningful Names**: class names are nouns, method names are verbs, booleans prefix with `is`/`has`/`can`.
- **Small Functions**: methods MUST NOT exceed 30 lines. Classes MUST NOT exceed 300 lines. If exceeded, refactor immediately.
- **Immutability**: domain models and value objects are immutable (`@Value` or `final` fields).
- **No Static Mutable State**: static fields are constants only.
- **No God Classes**: extract interfaces or new classes when limits are hit.
- **No Dead Code**: commented-out code and unused imports are forbidden.
- **No `System.out`**: use `@Slf4j` for all logging.
- **Null Safety**: avoid `null`. Use `Optional` for nullable returns. Use `@NonNull`/`@Nullable`.
- **Exception Handling**: domain-specific exceptions only. No generic `catch (Exception)` without rethrow/wrap.

## 4. Maven Multi-Module Rules

- The project uses Maven as the single build tool. No Gradle or other build tools.
- Modules: `domain`, `application`, `infrastructure`.
- The parent POM manages all dependency versions.
- Do NOT add framework dependencies to `domain`.
- Do NOT add SDK dependencies to `application`.
- `infrastructure` is the only module that may depend on external SDKs.

## 5. API & Kafka Contracts

### 5.1 REST
- All endpoints use standard HTTP verbs and plural nouns (`/api/videos`, `/api/channels`).
- All errors return RFC 7807 `application/problem+json`.
- `POST /api/videos/upload` MUST support `Idempotency-Key` header.
- All list endpoints MUST return paginated results (`page`, `size`, `totalElements`, `totalPages`).

### 5.2 Kafka
- Topic names use kebab-case, plural: `video-received`, `video-published`.
- Event payloads MUST be versioned (`schemaVersion`).
- Payloads MUST NOT contain binary data. Use storage paths/URIs only.
- Consumers MUST be idempotent. Duplicate events MUST NOT cause duplicate YouTube uploads.
- DLQ topics: `video-received-dlq`, `video-published-dlq` (after 3 retries).
- If Kafka is unavailable, events MUST be persisted to the outbox (`outbox_events` table).

## 6. Security Rules

- JWT Bearer tokens required on all endpoints except `/api/auth/**` and `/api/health`.
- Users MAY only access their own channels and videos.
- OAuth2 tokens (`access_token`, `refresh_token`, `token_expiry`) MUST be encrypted at rest using AES/GCM/NoPadding. Key source: `${ENCRYPTION_KEY}` or `${JWT_SECRET}`.
- YouTube scope is strictly limited to `https://www.googleapis.com/auth/youtube.upload`.
- Multipart uploads MUST be validated for file type (`video/*`), size (max 100MB), and filename sanitization.
- Production requires HTTPS. Local HTTP is permitted only for `localhost`.

## 7. Resilience Rules

- **Retry**: exponential backoff with jitter, initial 1s, max 30s, 3 attempts.
- **Circuit Breakers**: YouTube and Gemini use Resilience4j. Failure threshold 50% over 10s, wait 60s.
- **Gemini Fallback**: on failure, return `"Title for {filename}"` and `"Description for {filename}"`. Upload proceeds.
- **Storage Fallback**: MinIO unreachable in production returns 503. Local fallback only when `storage.type=local`.
- **Kafka Fallback**: persist to outbox in production; in-memory buffer acceptable for development only.

## 8. Observability Rules

- All logs use JSON format with `tenantId` and `correlationId`.
- Correlation ID flows: HTTP request → use case → adapter → Kafka event → consumer.
- Required metrics: `upload.duration`, `gemini.latency`, `youtube.quota.used`, `kafka.publish.latency`.
- `GET /api/health` MUST report PostgreSQL, Kafka, MinIO, and YouTube API connectivity.

## 9. Testing Rules

- **Unit Tests**: all `*UseCase.java` classes tested with mocked ports. Target 90%+ coverage for `domain` and `application`. No external SDKs, no DB, no Kafka, no HTTP.
- **Integration Tests**: all adapters tested with Testcontainers (PostgreSQL, Kafka, MinIO).
- **Contract Tests**: REST controllers validated against OpenAPI 3.0 schema.
- **No External Calls in Unit Tests**: integration tests are the ONLY place real external calls are permitted.
- Test classes follow the same Clean Code principles. Use `@DisplayName` for readability.

## 10. What Agents MUST NOT Do

1. Add Spring, JPA, Kafka, or SDK dependencies to `domain`.
2. Add framework dependencies to `application`.
3. Load full video files into memory (`byte[]`, `ByteArrayOutputStream` for complete files).
4. Hardcode secrets, API keys, or tokens.
5. Implement features listed in Constitution §11 (transcoding, thumbnails, scheduling, WebSocket/SSE, admin UI, batch upload, multi-language).
6. Use synchronous HTTP waits for YouTube publish.
7. Emit Kafka events with binary payloads.
8. Skip tenant isolation in any query, path, or event.
9. Use `System.out` or `System.err` for logging.
10. Write manual getters/setters/constructors when Lombok can express them.
11. Create classes larger than 300 lines or methods larger than 30 lines without refactoring.
12. Catch generic `Exception` without rethrow or wrapping.
13. Modify database schema without Flyway/Liquibase migrations.
14. Commit `.env` files or secrets to version control.

## 11. Workflow for Implementing a Feature

1. **Read**: Constitution, Architecture, PRD. Identify the governing rules.
2. **Design**: Define ports (interfaces) in `domain/port`. No implementation details.
3. **Implement Use Case**: in `application/service`. Orchestrate ports. Keep it small.
4. **Implement Adapter**: in `infrastructure/adapter`. Implement the port. Handle external SDK details here.
5. **Wire**: in `infrastructure/config`. Beans, security, Kafka, storage.
6. **Test**: unit test the use case, integration test the adapter, contract test the controller.
7. **Verify**: run `mvn clean verify`. Ensure no dependency violations in `domain`.
8. **Document**: update `arq/architecture.md` if new adapters or ports are introduced.

## 12. Amendment Process

If a requested change violates the Constitution, do not implement it. Instead:
1. Explain the violation and cite the specific section.
2. Propose an amendment following Constitution §12:
   - Written proposal documenting the change, rationale, and impact.
   - Review and approval by project maintainers.
   - Version bump with changelog entry.
3. Only after amendment approval, proceed with implementation.

## 13. Code Review Checklist

Before submitting any code, verify:
- [ ] `domain` module has zero Spring/JPA/Kafka/SDK dependencies.
- [ ] All new classes use Lombok annotations instead of manual boilerplate.
- [ ] TenantId is present in every repository call, storage path, and Kafka event.
- [ ] No `byte[]` full file loads anywhere.
- [ ] No hardcoded secrets or URLs.
- [ ] All external calls have retry + circuit breaker where applicable.
- [ ] All list endpoints are paginated.
- [ ] All errors return RFC 7807 format.
- [ ] All tests pass (`mvn clean verify`).
- [ ] No class exceeds 300 lines, no method exceeds 30 lines.
- [ ] No `System.out` or commented-out code.
- [ ] Kafka topics use kebab-case and payloads contain no binary data.
