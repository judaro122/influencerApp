# InfluencerAPP — Development Plan

**Version:** 1.0.0
**Status:** Draft
**Last Updated:** 2026-07-28
**Derived from:** PRD §10, Architecture Specification, Audit Report (`docs/specs/audit-report.md`)

---

## 1. Overview

This development plan defines the phased approach to building the InfluencerAPP platform. It maps the PRD implementation roadmap (§10) to the specification suite, incorporating audit findings and constitution constraints. All phases assume `PASSED_WITH_WARNINGS` specification readiness.

---

## 2. Pre-Development Gates

Before writing any application code, the following gates must be satisfied:

| Gate | Document | Status |
|------|----------|--------|
| Specification audit passed | `docs/specs/audit-report.md` | ✅ `PASSED_WITH_WARNINGS` |
| Constitution ratified | `docs/specs/constitution.md` | ✅ Ratified |
| OpenAPI schema finalized | `docs/specs/arq/openapi.yaml` | ✅ Remediated |
| Data model complete | `docs/specs/arq/data-model.md` | ✅ Remediated |
| Kafka event schemas finalized | `docs/specs/arq/kafka-events.md` | ✅ Remediated |
| Security spec approved | `docs/specs/security-spec.md` | ✅ Ready |
| Infrastructure spec approved | `docs/specs/infra-spec.md` | ✅ Ready |
| Test strategy approved | `docs/specs/test-spec.md` | ✅ Ready |
| Docker Compose validated | `docs/specs/infra-spec.md` §2 | ✅ Ready |
| All CRITICAL/HIGH audit findings remediated | `docs/specs/audit-report.md` | ✅ Complete |

---

## 3. Implementation Phases

### Phase 1: Foundation (Weeks 1–2)

**Goal:** Scaffold the project, set up infrastructure, and implement the domain layer.

| Task | Specification Reference | Acceptance Criteria |
|------|------------------------|---------------------|
| Project scaffolding (Maven multi-module) | Constitution §9, PRD §7.1 | `domain/`, `application/`, `infrastructure/` modules compile |
| Parent POM with dependency management | Constitution §9 | All module versions managed centrally |
| Domain layer: `Video`, `Channel`, `Tenant`, `User` aggregates | Architecture §7.1, Data Model §2.1 | Immutable value objects with `@Value`, no framework deps |
| Domain layer: Ports (`ObjectStoragePort`, `AITextGenerationPort`, `YouTubeUploadPort`, `KafkaProducerPort`, `VideoRepository`, `ChannelRepository`) | Architecture §7.2 | Interfaces in `domain.port.outbound` |
| Domain layer: Inbound ports (`UploadVideoUseCase`, `RegisterChannelUseCase`, `GetVideoStatusUseCase`, `RegisterUserUseCase`, `LoginUseCase`, `ListChannelsUseCase`, `GenerateMetadataUseCase`) | Architecture §6.3 | Interfaces in `domain.port.inbound` |
| Domain layer: Exceptions (`DomainException`, `TenantIsolationViolationException`) | Constitution §3.4 | Custom exceptions in `domain.exception` |
| Database schema: Flyway V1–V4 migrations | Infra-spec §3 | All tables created, `outbox_events` includes `tenant_id` and unique constraint |
| Docker Compose dev environment | Infra-spec §2 | `docker compose up` starts all services |
| Unit tests for domain models and value objects | Test-spec §5.1 | 90%+ coverage on domain layer |

**Audit items addressed:** AUDIT-001 (exception naming), AUDIT-004 (ports), AUDIT-014/015 (US traceability)

---

### Phase 2: Core Features (Weeks 3–4)

**Goal:** Implement user registration/login, channel registration, and the AI metadata generation pipeline.

| Task | Specification Reference | Acceptance Criteria |
|------|------------------------|---------------------|
| `RegisterUserUseCase` implementation | Architecture §6.3 | User created with hashed password, `tenantId` assigned |
| `LoginUseCase` implementation | Architecture §6.3 | JWT returned with `sub`, `tenantId`, `exp` claims |
| JWT authentication filter (`JwtAuthFilter`) | Security-spec §2, Architecture §12.1 | `401` returned for missing/invalid tokens |
| Tenant extraction from JWT | Security-spec §3 | `TenantId` set on `SecurityContext` for all requests |
| `RegisterChannelUseCase` implementation | Architecture §6.3, Security-spec §4 | OAuth2 code exchanged, tokens encrypted with AES/GCM, stored as `bytea` |
| `GeminiAdapter` implementing `AITextGenerationPort` | Integrations-spec §3, Architecture §11.1 | Calls Gemini API, returns title + description |
| `GenerateMetadataUseCase` implementation | Architecture §6.3 | Orchestrates Gemini call, handles fallback |
| Resilience4j circuit breaker for Gemini | Integrations-spec §3.2, Architecture §11.3 | 50% failure over 10s, 60s wait |
| Gemini fallback to placeholder text | Constitution §6, Integrations-spec §3.4 | `"Title for {filename}"`, `"Description for {filename}"` |
| `TokenEncryptionService` (AES/GCM/NoPadding) | Security-spec §4, Architecture §11.3 | Encrypt/decrypt tokens in-memory only |
| MinIO storage adapter (`MinIOStorageAdapter`) | Integrations-spec §5, Architecture §9.1 | Streaming multipart upload, tenant-prefixed paths |
| Unit tests for all use cases | Test-spec §5.1 | Mocked ports, 90%+ coverage |
| Integration tests for `GeminiAdapter` and `MinIOStorageAdapter` | Test-spec §5.2 | Testcontainers for MinIO |
| Contract tests for auth and channel endpoints | Test-spec §5.3 | OpenAPI schema validation |

**Audit items addressed:** AUDIT-005 (correlation ID), AUDIT-006 (correlationId type), AUDIT-012/013 (500/429 responses)

---

### Phase 3: Upload Pipeline (Weeks 5–6)

**Goal:** Implement the video upload pipeline, YouTube publishing, and Kafka event emission.

| Task | Specification Reference | Acceptance Criteria |
|------|------------------------|---------------------|
| `UploadVideoUseCase` implementation | Architecture §6.3 | Validates input, stores to MinIO, persists `VideoEntity` in `RECEIVED` status |
| `IdempotencyKey` header handling | Architecture §7.3, Data Model §3.3 | Duplicate keys within 24h return cached response |
| `VideoEventProducer` (Kafka producer) | Architecture §7.2, Kafka-events §4 | Emits `video-received` event after storage |
| `VideoProcessingService` (orchestration) | Architecture §4 | Calls `GenerateMetadataUseCase`, transitions to `PROCESSING` → `UPLOADING` |
| `YouTubeUploadAdapter` implementing `YouTubeUploadPort` | Integrations-spec §4, Architecture §11.2 | Streaming upload, token refresh, resumable upload |
| `PublishToYouTubeUseCase` implementation | Architecture §6.3 | Orchestrates YouTube upload, transitions to `PUBLISHED` |
| `VideoEventProducer` for `video-published` event | Kafka-events §4.3 | Emits `video-published` event after successful YouTube upload |
| `OutboxPublisher` implementation | Kafka-events §7.3, Data Model §3.3 | Polls `outbox_events`, publishes to Kafka, handles retry |
| `OutboxEvents` table with all columns (incl. `tenant_id`, `aggregate_id`, `event_type`, `schema_version`, `error_message`) | Data Model §3.3, Infra-spec §3.6 | Unique constraint `(aggregate_id, event_type)` enforced |
| `ProcessedEvents` table for consumer idempotency | Data Model §3.3, Kafka-events §5.4 | Unique constraint on `event_id` |
| `VideoEventConsumer` (Kafka consumer) | Architecture §7.2 | Idempotent processing, DLQ routing after 3 failures |
| Resilience4j circuit breaker for YouTube | Integrations-spec §4.3 | 50% failure over 10s, 60s wait |
| YouTube upload streaming (no in-memory buffering) | Integrations-spec §6, Constitution §2.3 | `InputStream` passed directly from MinIO to YouTube |
| Unit tests for all upload pipeline use cases | Test-spec §5.1 | Mocked ports |
| Integration tests for `YouTubeUploadAdapter`, `VideoEventProducer`, `VideoEventConsumer` | Test-spec §5.2 | Testcontainers for Kafka, MinIO |
| Contract tests for video endpoints | Test-spec §5.3 | Validate `Idempotency-Key` header, RFC 7807 errors |

**Audit items addressed:** AUDIT-007 (idempotencyKey), AUDIT-009 (DLQ routing), AUDIT-010 (unique constraint), AUDIT-016 (test traceability)

---

### Phase 4: Production Readiness (Weeks 7–8)

**Goal:** Harden the application for production deployment, add monitoring, and complete testing.

| Task | Specification Reference | Acceptance Criteria |
|------|------------------------|---------------------|
| Docker Compose production profile | Infra-spec §2.4, §2.5 | All services defined, secrets via Docker secrets |
| `.env` file excluded from version control | Constitution §10.3 | `.gitignore` includes `.env` |
| Health check endpoint (`/api/health`) | Constitution §9.3, Architecture §14.3 | Reports PostgreSQL, Kafka, MinIO, YouTube API status |
| Micrometer metrics instrumentation | Observability-spec §4, Integrations-spec §7 | All 13 metrics emitted via `MeterRegistry` |
| JSON structured logging with `correlationId` and `tenantId` | Observability-spec §2, Constitution §9 | Logstash encoder, all INFO+ logs include correlation ID |
| `X-Correlation-Id` header propagation | Observability-spec §3 | Flows from HTTP ingress through Kafka to consumer logs |
| Circuit breaker state monitoring | Integrations-spec §7, Observability-spec §4.8 | `circuit_breaker.state` gauge emitted |
| DLQ monitoring | Kafka-events §6, Observability-spec §4.12 | `kafka.dlq.size` gauge, alerts on non-zero |
| Outbox pending monitoring | Kafka-events §7, Observability-spec §4.11 | `kafka.outbox.pending` gauge, alerts on unbounded growth |
| JaCoCo coverage enforcement in CI | Test-spec §4.5 | Build fails if domain/application coverage < 90% |
| Full integration test suite | Test-spec §5.2 | All adapters tested with Testcontainers |
| SonarQube quality gate | Test-spec §4.4 | No critical issues, Lombok usage consistent |
| API documentation via SpringDoc | PRD §7.2, OpenAPI 3.0 | `springdoc-openapi` generates `/api/docs.html` |
| Security hardening review | Security-spec §7 | No secrets in code, HTTPS in production, token encryption verified |

**Audit items addressed:** AUDIT-005 (X-Correlation-Id in OpenAPI), AUDIT-008 (tenantId format), AUDIT-017 (tenantId in response)

---

## 4. Dependency Graph

```
Phase 1 (Foundation)
 ├── Domain layer (aggregates, ports, exceptions)
 ├── Flyway migrations V1–V4
 └── Docker Compose dev environment
       │
       ▼
Phase 2 (Core Features)
 ├── JWT authentication filter
 ├── Auth endpoints (register, login)
 ├── Channel registration (OAuth2 + token encryption)
 ├── Gemini adapter + circuit breaker + fallback
 ├── MinIO storage adapter (streaming upload)
 └── Unit + integration + contract tests
        │
        ▼
Phase 3 (Upload Pipeline)
 ├── Upload use case + idempotency
 ├── YouTube upload adapter + streaming
 ├── Kafka producer + consumer + OutboxPublisher
 ├── DLQ routing + consumer idempotency
 └── All pipeline tests
       │
       ▼
Phase 4 (Production Readiness)
 ├── Monitoring (Micrometer, health checks, logging)
 ├── Docker Compose production profile
 ├── CI/CD pipeline (JaCoCo, SonarQube)
 └── Security hardening
```

---

## 5. Risk Register

| Risk | Impact | Probability | Mitigation | Phase |
|------|--------|-------------|------------|-------|
| YouTube OAuth2 complexity | High | Medium | Detailed setup guide, WireMock in tests | Phase 2 |
| YouTube quota exhaustion | High | Low | Circuit breaker, quota monitoring, user notification | Phase 3 |
| Gemini free tier rate limits | Medium | Medium | Circuit breaker opens before quota exhaustion, fallback placeholder | Phase 2 |
| Large file memory pressure | High | Medium | Streaming uploads enforced, no `byte[]` full loads | Phase 3 |
| Token security breach | High | Low | AES/GCM encryption at rest, no plaintext logging | Phase 2 |
| Kafka unavailability | Medium | Low | Outbox pattern with persistent buffering | Phase 3 |
| Cross-tenant data leakage | Critical | Low | `TenantIsolationViolationException` at every layer, `tenant_id` in all queries | Phase 1 |
| Outbox table schema mismatch | High | Medium | Migration SQL and data model spec aligned (AUDIT-002/003 remediated) | Phase 3 |

---

## 6. Open Items

| Item | Owner | Resolution |
|------|-------|-----------|
| Video file retention policy | Product | PRD §12 open question; no lifecycle automation in v1.0 |
| 500/429 RFC 7807 responses per endpoint | Dev | Add in follow-up pass (AUDIT-012/013) |
| `tenantId` in `VideoStatusResponse` necessity | Dev/Product | Evaluate if client needs this field (AUDIT-017) |
| Same-key-same-payload idempotency in 201 response | Dev | Add note to OpenAPI 201 response (AUDIT-018) |
| Absolute URIs for `instance` in ProblemDetail | Dev | Document convention or update examples (AUDIT-019) |

---

## 7. Definition of Done per Phase

A phase is complete when:

1. All tasks in the phase are implemented and unit-tested.
2. Integration tests pass with Testcontainers.
3. Contract tests validate against the OpenAPI schema.
4. No CRITICAL or HIGH audit findings remain open.
5. Code coverage for domain and application layers is ≥90%.
6. SonarQube has no critical issues.
7. The specification audit report is re-validated.

---

*Version: 1.0.0*
*Status: Draft*
*Last Updated: 2026-07-28*