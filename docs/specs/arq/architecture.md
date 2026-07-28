# InfluencerAPP - Architecture Specification

## 1. Alignment & Constitution Mapping

This document derives the system architecture from the Project Constitution (v1.0) and Product Requirements Document. Every rule below is traceable to a constitutional requirement or PRD feature.

| Constitution Section | Architecture Decision |
|---------------------|----------------------|
| §1 Hexagonal Architecture Strictly Enforced | Pure ports & adapters, zero framework dependencies in domain |
| §2 Multi-Tenancy by Default | Tenant ID injected at API gateway layer, flows through all ports |
| §3 Streaming Over Buffering | All upload/download operations use `InputStream` / chunked transfer, no `byte[]` full loads |
| §4 Eventual Consistency via Events | Upload pipeline is async via Kafka; no synchronous HTTP wait for YouTube |
| §5 Secrets Never in Code | All credentials via env vars / secret manager; JCE encryption key via env |
| §6 Free-Tier Constraints | Resilience4j circuit breakers + fallback placeholder text for Gemini |
| §7 Clean Code & Maintainability | Single responsibility, meaningful names, small classes/methods, no dead code |
| §8 Lombok for Boilerplate Elimination | `@Data`, `@Builder`, `@Slf4j`, `@Value`, `@With`, `@RequiredArgsConstructor` standard across all layers |
| §9 Maven as Canonical Build | Multi-module Maven project; parent POM manages versions; no Gradle |
| §11 Out of Scope | Transcoding, thumbnails, scheduling, WebSocket, admin UI, batch upload, multi-language |

---

## 2. Architectural Style

**Hexagonal Architecture (Ports & Adapters)** with strict layer boundaries enforced via Maven module separation or Java package-level compile rules.

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Infrastructure Layer                        │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌────────────┐ │
│  │ HTTP        │  │ YouTube     │  │ Gemini      │  │ Kafka      │ │
│  │ Adapters    │  │ Adapter     │  │ Adapter     │  │ Adapters   │ │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘  └─────┬──────┘ │
│         │                │                │                │        │
│  ┌──────┴──────┐  ┌──────┴──────┐  ┌──────┴──────┐  ┌─────┴──────┐ │
│  │ Storage     │  │ DB          │  │ Security    │  │ Monitoring │ │
│  │ Adapters    │  │ Repositories│  │ Adapters    │  │ Adapters   │ │
│  └─────────────┘  └─────────────┘  └─────────────┘  └────────────┘ │
└───────────────────────────┬─────────────────────────────────────────┘
                            │ uses
┌───────────────────────────▼─────────────────────────────────────────┐
│                      Application Layer                              │
│  ┌─────────────────────────────────────────────────────────────────┐│
│  │ Use Cases (orchestration, transaction scripts)                 ││
│  │ - UploadVideoUseCase                                          ││
│  │ - GenerateMetadataUseCase                                     ││
│  │ - PublishToYouTubeUseCase                                     ││
│  │ - RegisterChannelUseCase                                      ││
│  │ - RegisterUserUseCase                                         ││
│  │ - LoginUseCase                                                ││
│  │ - ListChannelsUseCase                                         ││
│  └─────────────────────────────────────────────────────────────────┘│
└───────────────────────────┬─────────────────────────────────────────┘
                            │ depends on
┌───────────────────────────▼─────────────────────────────────────────┐
│                        Domain Layer                                 │
│  ┌─────────────────────────────────────────────────────────────────┐│
│  │ Entities (Aggregates)                                          ││
│  │ - Video (Aggregate Root)                                       ││
│  │ - Channel (Aggregate Root)                                     ││
│  │ - Tenant (Entity)                                              ││
│  │ - User (Entity)                                                ││
│  │                                                                ││
│  │ Ports (Interfaces)                                             ││
│  │ - ObjectStoragePort                                            ││
│  │ - AITextGenerationPort                                         ││
│  │ - YouTubeUploadPort                                            ││
│  │ - VideoRepository                                              ││
│  │ - ChannelRepository                                            ││
│  │ - KafkaProducerPort                                            ││
│  │                                                                ││
│  │ Value Objects                                                  ││
│  │ - TenantId, VideoId, ChannelId                                 ││
│  │ - FileMetadata (filename, size, mimeType)                      ││
│  │ - YouTubeUrl, VideoStatus                                      ││
│  └─────────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────────┘
```

---

## 3. Layer Boundaries & Allowed Dependencies

| Layer | Allowed Dependencies | Forbidden |
|-------|---------------------|-----------|
| `domain` | Java 17+ standard library, `jakarta.validation` | Spring, JPA, Kafka, HTTP clients, AI SDKs, persistence frameworks |
| `application` | `domain`, `spring-context`, `spring-boot` configuration | `spring-web`, `spring-data`, `spring-kafka`, any SDK |
| `infrastructure` | `application`, `domain`, all external SDKs | None — this is the adapter layer |

### Enforcement
- Maven multi-module: `domain` → no dependency on `application` or `infrastructure`
- Alternatively, package-private visibility + ArchUnit tests enforcing no `spring-*` imports in `domain`

---

## 4. Package Structure

```
com.influencerapp
├── domain
│   ├── model
│   │   ├── Tenant.java
│   │   ├── User.java
│   │   ├── Channel.java
│   │   └── Video.java
│   ├── port
│   │   ├── inbound
│   │   │   ├── UploadVideoUseCase.java
│   │   │   ├── RegisterChannelUseCase.java
│   │   │   └── GetVideoStatusUseCase.java
│   │   └── outbound
│   │       ├── ObjectStoragePort.java
│   │       ├── AITextGenerationPort.java
│   │       ├── YouTubeUploadPort.java
│   │       ├── VideoRepository.java
│   │       ├── ChannelRepository.java
│   │       └── KafkaProducerPort.java
│   └── exception
│       ├── DomainException.java
│       └── TenantIsolationViolationException.java
├── application
│   ├── service
│   │   ├── UploadVideoService.java
│   │   ├── MetadataGenerationService.java
│   │   ├── YouTubePublishingService.java
│   │   └── ChannelRegistrationService.java
│   ├── event
│   │   ├── VideoReceivedEvent.java
│   │   └── VideoPublishedEvent.java
│   └── dto
│       ├── VideoUploadRequest.java
│       └── VideoStatusResponse.java
└── infrastructure
    ├── adapter
    │   ├── http
    │   │   ├── VideoController.java
    │   │   ├── AuthController.java
    │   │   └── ChannelController.java
    │   ├── storage
    │   │   ├── MinIOStorageAdapter.java
    │   │   └── LocalStorageAdapter.java
    │   ├── ai
    │   │   └── GeminiAdapter.java
    │   ├── youtube
    │   │   └── YouTubeUploadAdapter.java
    │   ├── kafka
    │   │   ├── VideoEventProducer.java
    │   │   └── VideoEventConsumer.java
    │   ├── security
    │   │   ├── JwtAuthFilter.java
    │   │   └── TokenEncryptionService.java
    │   └── monitoring
    │       └── HealthCheckIndicator.java
    ├── config
    │   ├── KafkaConfig.java
    │   ├── SecurityConfig.java
    │   └── StorageConfig.java
    ├── repository
    │   ├── JpaVideoRepository.java
    │   └── JpaChannelRepository.java
    └── entity
        ├── VideoEntity.java
        └── ChannelEntity.java
```

---

## 5. Clean Code & Lombok Conventions

### 5.1 Lombok Usage
Lombok is the canonical tool for eliminating Java boilerplate. All classes MUST use Lombok annotations where applicable.

| Annotation | Usage | Layer |
|------------|-------|-------|
| `@Value` | Immutable domain models and value objects | domain |
| `@Data` | Mutable DTOs and request/response objects | application, infrastructure |
| `@Builder` | Complex object construction, especially DTOs and entities | all |
| `@Slf4j` | Logging in all classes that log | all |
| `@RequiredArgsConstructor` | Constructor injection in adapters and services | application, infrastructure |
| `@With` | Non-destructive mutation for value objects | domain |
| `@NoArgsConstructor(force = true)` | JPA entities only (Hibernate requirement) | infrastructure |

### 5.2 Clean Code Rules
- **Single Responsibility**: One class = one reason to change.
- **Meaningful Names**: Class names are nouns, method names are verbs, booleans prefix with `is`/`has`/`can`.
- **Small Functions**: Methods MUST NOT exceed 30 lines. Classes MUST NOT exceed 300 lines.
- **Immutability**: Domain models and value objects are immutable. Use `@Value` or `final` fields.
- **No Static Mutable State**: Static fields are constants only.
- **No God Classes**: If a class grows beyond limits, extract interfaces or new classes.
- **No Dead Code**: Commented-out code and unused imports are forbidden.
- **No `System.out`**: Use `@Slf4j` for all logging.
- **Null Safety**: Avoid `null`. Use `Optional` for nullable returns. Use `@NonNull`/`@Nullable`.
- **Exception Handling**: Domain-specific exceptions only. No generic `catch (Exception)` without rethrow/wrap.
- **Test Readability**: Test method names describe scenario + expected outcome. Use `@DisplayName`.

---

## 6. Multi-Tenancy Strategy

**Principle**: Tenant isolation is enforced at the port boundary. No adapter receives unbound domain operations.

### 6.1 Tenant Identity
- `TenantId` is a `@Embeddable` value object in the domain.
- Extracted from authenticated JWT principal (`sub` claim) at the HTTP adapter layer.
- Injected into every repository call and storage path.

### 6.2 Database Isolation
- Every JPA entity includes `tenant_id` column.
- All repository queries use `@TenantId` parameter; `WHERE tenant_id = :tenantId` is mandatory.
- Spring Data JPA filters enforce tenant scoping via `@EntityGraph` or interceptor.

### 6.3 Storage Isolation
- MinIO object keys: `tenants/{tenantId}/videos/{videoId}/{filename}`
- Local fallback paths: `./storage/tenants/{tenantId}/videos/{...}`
- No storage operation bypasses tenant prefix.

### 6.4 Kafka Isolation
- Event payloads include `tenantId` field at top level.
- Consumer filters process only events matching its tenant context (if shared topics) OR uses separate topic prefixes (recommended: `{tenantId}-video-received`).

---

## 7. Domain Model

### 6.1 Aggregates

**Video** (Aggregate Root)
- `VideoId` (value object)
- `TenantId` (value object)
- `ChannelId` (value object)
- `FileMetadata` (filename, size, mimeType, checksum)
- `StoragePath` (value object)
- `Metadata` (title, description)
- `Status` (enum: RECEIVED, PROCESSING, UPLOADING, PUBLISHED, FAILED)
- `YouTubeVideoId` (value object)
- `CreatedAt`, `UpdatedAt`

**Channel** (Aggregate Root)
- `ChannelId` (value object)
- `TenantId` (value object)
- `YouTubeChannelId`
- `YouTubeChannelTitle`
- `EncryptedTokens` (access_token, refresh_token, token_expiry) — never plaintext
- `Scope` (fixed to `youtube.upload`)

### 6.2 Ports (Outbound)
- `ObjectStoragePort`: `store(InputStream, String key, String contentType)`, `retrieve(String key)`, `delete(String key)`
- `AITextGenerationPort`: `generateTitleAndDescription(FileMetadata) returns Metadata`
- `YouTubeUploadPort`: `upload(StoragePath, Metadata, Channel) returns YouTubeVideoId`
- `KafkaProducerPort`: `publish(String topic, Object event)`
- `VideoRepository`, `ChannelRepository`: standard CRUD + tenant-scoped queries
### 6.3 Ports (Inbound)

- `UploadVideoUseCase`: `execute(TenantId, MultipartFile, ChannelId) returns VideoId`
- `RegisterChannelUseCase`: `execute(TenantId, String authCode) returns ChannelId`
- `GetVideoStatusUseCase`: `execute(TenantId, VideoId) returns VideoStatusResponse`
- `RegisterUserUseCase`: `execute(RegisterRequest) returns UserId`
- `LoginUseCase`: `execute(LoginRequest) returns JwtToken`
- `ListChannelsUseCase`: `execute(TenantId) returns List<ChannelResponse>`
- `GenerateMetadataUseCase`: `execute(TenantId, FileMetadata) returns VideoMetadata`

---

## 8. API Design

### 7.1 Endpoint Contract

| Method | Path | Auth | Request | Response | Notes |
|--------|------|------|---------|----------|-------|
| POST | `/api/auth/register` | No | `{email, password}` | `{id, email}` | |
| POST | `/api/auth/login` | No | `{email, password}` | `{token, expiresIn}` | Returns JWT |
| GET | `/api/health` | No | — | `{status, components}` | |
| POST | `/api/channels/register` | Yes | OAuth2 callback body | `{channelId, title}` | |
| GET | `/api/channels` | Yes | — | `{content: [...], page, size, totalElements}` | Paginated |
| POST | `/api/videos/upload` | Yes | `multipart/form-data` + `Idempotency-Key` header | `{videoId}` | Max 100MB |
| GET | `/api/videos/{videoId}` | Yes | — | `{id, status, metadata, youtubeUrl}` | |
| GET | `/api/videos` | Yes | — | `{content: [...], page, size, totalElements}` | Paginated |

### 7.2 RFC 7807 Error Responses
All errors return `application/problem+json`:
```json
{
  "type": "https://influencerapp/errors/validation-error",
  "title": "Validation Failed",
  "status": 400,
  "detail": "File size exceeds maximum of 100MB",
  "instance": "/api/videos/upload"
}
```

### 7.3 Idempotency
- `POST /api/videos/upload` reads `Idempotency-Key` header.
- Key is stored in `idempotency_keys` table with `tenant_id`, `response_body`, `created_at`.
- Duplicate keys within 24h return cached response.

### 7.4 Pagination
All list endpoints return:
```json
{
  "content": [...],
  "page": 0,
  "size": 20,
  "totalElements": 45,
  "totalPages": 3
}
```

---

## 9. Kafka Event Design

### 8.1 Topic Naming (kebab-case, plural)
| Topic | Purpose | Key |
|-------|---------|-----|
| `video-received` | Emitted after successful upload and validation | `videoId` |
| `video-published` | Emitted after YouTube upload completes | `videoId` |

### 8.2 Event Schema (Avro / JSON Schema versioned)
```json
{
  "schemaVersion": "1.0.0",
  "tenantId": "tenant-123",
  "correlationId": "req-uuid",
  "videoId": "vid-456",
  "channelId": "ch-789",
  "timestamp": "2026-07-28T17:00:00Z",
  "payload": {
    "storagePath": "tenants/tenant-123/videos/vid-456/file.mp4",
    "fileName": "file.mp4",
    "fileSize": 52428800,
    "mimeType": "video/mp4"
  }
}
```

**Constraint**: Payloads contain no binary data. Video references use storage URIs only.

### 8.3 Consumer Idempotency
- Consumers maintain processed event IDs in a deduplication store (DB table `processed_events` with unique constraint on `event_id`).
- Duplicate delivery does not trigger duplicate YouTube uploads.

### 8.4 Dead Letter Queue (DLQ)
- Topics: `video-received-dlq`, `video-published-dlq`
- After 3 processing failures (exponential backoff exhausted), events are routed to DLQ for manual inspection.

### 8.5 Unavailability Buffering
- **Development**: In-memory `BlockingQueue` with retry on reconnect.
- **Production**: Persistent Outbox pattern.
  - Table `outbox_events` (id, topic, payload, status, retry_count, created_at).
  - Separate `OutboxPublisher` polls table and publishes to Kafka.
  - Transaction: use case saves domain state + outbox event in same DB transaction.

---

## 10. Storage Strategy

### 9.1 MinIO (Default, Production)
- **Adapter**: `MinIOStorageAdapter` implements `ObjectStoragePort`.
- **Bucket per tenant**: `tenant-{tenantId}` or single bucket with tenant-prefixed paths.
- **Upload**: `TransferManager` with multipart upload for files >5MB; chunk size configurable.
- **Download**: Streaming `ResponseBody` input stream, never loaded entirely into memory.

### 9.2 Local Filesystem (Development / Explicit Config)
- **Adapter**: `LocalStorageAdapter` implements `ObjectStoragePort`.
- Activated only when `storage.type=local`.
- Path: `./storage/tenants/{tenantId}/videos/{videoId}/`.

### 9.3 Constitution Compliance
- No `byte[]` full file loads anywhere in the upload/download pipeline.
- All I/O uses `java.io.InputStream` or `java.nio.channels.ReadableByteChannel`.

---

## 11. AI / Gemini Integration

### 10.1 Adapter
- `GeminiAdapter` implements `AITextGenerationPort`.
- Model: `gemini-2.0-flash` (free tier compatible).

### 10.2 Resilience
- **Circuit Breaker**: Resilience4j with failure threshold 50% over 10s, wait 60s.
- **Retry**: Exponential backoff with jitter, initial 1s, max 30s, 3 attempts.
- **Fallback**: On circuit open or exhaustion, return:
  - Title: `"Title for {filename}"`
  - Description: `"Description for {filename}"`
- Upload proceeds with placeholder metadata. Failure is logged but not fatal.

### 10.3 Rate Limit Handling
- Free tier quotas are architectural constraints.
- Circuit breaker opens before quota exhaustion impacts other tenants.
- `gemini.latency` metric emitted per request.

---

## 12. YouTube Integration

### 11.1 OAuth2 Flow
- User initiates via `GET /api/channels/register`.
- System generates authorization URL with scope: `https://www.googleapis.com/auth/youtube.upload`.
- Google callback hits `POST /api/channels/register` with auth code.
- System exchanges code for tokens and stores encrypted.

### 11.2 Token Management
- `YouTubeUploadAdapter` refreshes `access_token` transparently before upload if expired.
- Refresh uses `refresh_token` (never exposed to client after initial registration).
- Tokens stored encrypted in `channels` table.

### 11.3 Encryption
- Algorithm: AES/GCM/NoPadding.
- Key source: `${ENCRYPTION_KEY}` env var (falls back to `${JWT_SECRET}` if not set).
- Service: `TokenEncryptionService` in infrastructure security adapter.

### 11.4 Upload Execution
- Downloads video from storage as stream.
- Resumable upload via YouTube Data API v3.
- No synchronous wait; upload is fire-and-forget via Kafka consumer or async use case.
- If upload fails, retry up to 3 times with exponential backoff; then mark video `FAILED` and emit error event.

---

## 13. Security Architecture

### 12.1 Authentication
- JWT Bearer tokens required on all endpoints except `/api/auth/**` and `/api/health`.
- Token contains `sub` (userId), `tenantId`, `exp`.
- Signed with HS256 or RS256 using `${JWT_SECRET}`.

### 12.2 Authorization
- Every use case validates `TenantId` from token matches resource tenant.
- `TenantIsolationViolationException` thrown on cross-tenant access attempt.
- Channel ownership verified before YouTube operations.

### 12.3 Input Validation
- Multipart uploads validated for:
  - `Content-Type` (must be video/*)
  - `Content-Length` (max 100MB, configurable)
  - Filename sanitization (no path traversal)
- Bean Validation (`jakarta.validation`) on DTOs.

### 12.4 Transport Security
- Production: HTTPS only (TLS termination at load balancer / reverse proxy).
- Local: HTTP permitted for `localhost`.

---

## 14. Resilience & Failure Handling

### 13.1 Retry Policy
Applied to all external calls (Gemini, YouTube, MinIO, Kafka):
- Initial interval: 1s
- Max interval: 30s
- Multiplier: 2.0 (exponential)
- Max attempts: 3
- Jitter: ±10%

### 13.2 Circuit Breakers
- YouTube integration: failure threshold 50% over 10s, wait 60s.
- Gemini integration: failure threshold 50% over 10s, wait 60s.
- Open state: fast-fail with fallback behavior.

### 13.3 Graceful Degradation
| Failure | Behavior |
|---------|----------|
| Gemini unavailable | Use placeholder text; upload continues |
| MinIO unreachable (prod) | Return 503; do not proceed with upload |
| MinIO unreachable (`storage.type=local`) | Fall back to local filesystem |
| Kafka unavailable | Persist to outbox; retry on reconnect |
| YouTube quota exhausted | Circuit breaker opens; notify via metric + DLQ |

---

## 15. Observability

### 14.1 Structured Logging
- Format: JSON (Logstash encoder).
- Fields: `timestamp`, `level`, `service`, `tenantId`, `correlationId`, `message`, `error`.
- Correlation ID flows: HTTP request → use case → adapter → Kafka event → consumer.

### 14.2 Metrics (Micrometer / Prometheus)
| Metric | Type | Labels |
|--------|------|--------|
| `upload.duration` | Timer | tenantId, status |
| `gemini.latency` | Timer | tenantId |
| `youtube.quota.used` | Gauge | tenantId |
| `kafka.publish.latency` | Timer | topic |
| `storage.operation.errors` | Counter | adapter, operation |

### 14.3 Health Checks
Endpoint: `GET /api/health` returns:
```json
{
  "status": "UP",
  "components": {
    "postgresql": {"status": "UP"},
    "kafka": {"status": "UP"},
    "minio": {"status": "UP"},
    "youtubeApi": {"status": "UP"}
  }
}
```

---

## 16. Testing Strategy

### 15.1 Unit Tests (90%+ coverage target)
- All `*UseCase.java` classes tested with mocked ports (Mockito).
- No external SDKs in unit tests.
- No database, no Kafka, no HTTP calls.

### 15.2 Integration Tests (Testcontainers)
- **Adapters**: `MinIOStorageAdapterIT`, `GeminiAdapterIT`, `YouTubeUploadAdapterIT`.
- **Infrastructure**: Full Spring context with Testcontainers PostgreSQL, Kafka, MinIO.
- **Repository**: `JpaVideoRepositoryIT` with real PostgreSQL.

### 15.3 Contract Tests
- REST controllers validated against OpenAPI 3.0 schema (SpringDoc + Spring REST Docs or `restdocs-api-spec`).
- Request/response structure, status codes, headers (`Idempotency-Key`).

### 15.4 Out of Scope in Tests
- No external calls in unit tests.
- No real YouTube API in integration tests (use WireMock or recorded stubs).

---

## 17. Deployment Architecture

### 16.1 Containerization
- Single Dockerfile for Spring Boot application (JVM 17, Alpine base).
- Multi-stage build or plain `java -jar`.

### 16.2 Docker Compose (Development / Testing)
Services:
- `app` (Spring Boot)
- `postgres` (PostgreSQL 15+)
- `kafka` + `zookeeper` (Confluent or Bitnami)
- `minio` (single node)
- `init-migrations` (Flyway)

### 16.3 Database Migrations
- Flyway or Liquibase required.
- Migration scripts in `src/main/resources/db/migration/`.
- Run on application startup; no manual schema changes permitted.

### 16.4 Secrets Management
- All secrets via environment variables.
- No `.env` files committed to version control.
- Production: Docker secrets or external secret manager (HashiCorp Vault, AWS Secrets Manager).

---

## 18. Technology Stack (Canonical)

| Concern | Technology | Rationale |
|---------|-----------|-----------|
| Language | Java 17 | LTS, records, sealed classes |
| Framework | Spring Boot 3.x | Constitution mandate |
| Architecture | Hexagonal (Ports & Adapters) | Constitution §1 |
| Build Tool | Maven (multi-module) | Constitution §9 |
| Boilerplate | Lombok | Constitution §8 |
| Database | PostgreSQL 15+ | Constitution §3.3 |
| ORM | Spring Data JPA (Hibernate) | Entity mapping 1:1 with domain |
| Messaging | Apache Kafka | Constitution mandate |
| Storage | MinIO | S3-compatible, constitution mandate |
| AI | Google Generative AI (Gemini) | Free tier, constitution §6 |
| YouTube | YouTube Data API v3 | Constitution mandate |
| Resilience | Resilience4j | Circuit breakers, retry |
| Auth | Spring Security + JWT | Constitution §4.1 |
| Encryption | JCE (AES/GCM/NoPadding) | Constitution §4.3 |
| API Docs | SpringDoc OpenAPI 3.0 | PRD §7.2 |
| Testing | JUnit 5, Mockito, Testcontainers | Constitution §8 |
| Monitoring | Micrometer + Prometheus | Constitution §9 |

---

## 19. Out of Scope (Explicitly Excluded per Constitution §11)

The following are **MUST NOT** be implemented in v1.0:
1. Video transcoding or format conversion
2. Thumbnail generation or image processing
3. Scheduling or delayed publishing
4. WebSocket or Server-Sent Events for real-time client notifications
5. Admin dashboard or analytics UI
6. Batch/multi-video upload in a single request
7. Multi-language metadata generation

---

## 20. Amendment Process

Architectural changes to this specification follow the Constitution §12 process:
1. Written proposal documenting change, rationale, and impact.
2. Review and approval by project maintainers.
3. Version bump with changelog entry.

---

*Version: 1.0.0*  
*Status: Ratified*  
*Last Updated: 2026-07-28*
