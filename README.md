# InfluencerAPP

Backend service that automates YouTube content publishing. Users upload videos through a REST API; the system generates AI-powered titles and descriptions using Google Gemini (free tier), uploads the video to a pre-registered YouTube channel, and emits lifecycle events through Apache Kafka. Multi-tenancy is enforced by default: each authenticated user manages their own YouTube channels.

> **Current Status:** Phase 1 (Foundation), Phase 2 (Core Features), and Phase 3 (Upload Pipeline) are complete. See [`docs/specs/execution-state.md`](docs/specs/execution-state.md) for details.

## Key Features

- **Hexagonal Architecture**: strict ports & adapters with zero framework dependencies in the domain layer.
- **Multi-Tenancy**: tenant isolation in every query, storage path, and Kafka payload.
- **Streaming Uploads**: video files are never fully loaded into memory; chunked transfer to MinIO.
- **AI Metadata**: Gemini-powered title and description generation with circuit breaker and placeholder fallback.
- **Resilience**: Resilience4j circuit breakers, exponential backoff with jitter, graceful degradation.
- **Security**: AES/GCM/NoPadding token encryption for OAuth2 tokens.
- **Database Migrations**: Flyway-managed schema evolution (V1–V4).
- **JWT Authentication**: Bearer token auth with tenant extraction from `sub` claim.
- **Event-Driven Pipeline**: Kafka `video-received` and `video-published` events with outbox pattern, DLQ routing, and consumer idempotency.
- **Idempotent Uploads**: `Idempotency-Key` header support with 24h cached responses.
- **YouTube Publishing**: Streaming upload to YouTube Data API v3 with token refresh.
- **Testing**: unit tests for use cases, integration tests with Testcontainers, contract tests against OpenAPI.

> **Planned (Phase 4, not yet started):** Micrometer/Prometheus metrics, health checks for all dependencies, Docker production profile, CI/CD.

## Tech Stack

| Concern | Technology |
|---------|-----------|
| Language | Java 17 |
| Framework | Spring Boot 3.3.4 |
| Architecture | Hexagonal (Ports & Adapters) |
| Build | Maven (single module; multi-module split planned for Phase 4) |
| Boilerplate | Lombok |
| Database | PostgreSQL 15+ |
| Migrations | Flyway |
| Messaging | Apache Kafka |
| Storage | MinIO |
| AI | Google Generative AI (Gemini) |
| YouTube | YouTube Data API v3 |
| Resilience | Resilience4j |
| Encryption | JCE (AES/GCM/NoPadding) |
| Docs | SpringDoc OpenAPI 3.0 |
| Testing | JUnit 5, Mockito, Testcontainers |
| Monitoring | Micrometer + Prometheus |

## Prerequisites

- Java 17+ (Temurin or OpenJDK)
- Maven 3.9+
- Docker & Docker Compose
- Google Cloud Console project with YouTube Data API v3 enabled
- Gemini API key

## Quick Start

### 1. Clone and build

```bash
git clone https://github.com/your-org/influencerAPP.git
cd influencerAPP
mvn clean test
```

### 2. Start with Docker Compose

```bash
# Build the application image (no cache)
docker compose build --no-cache app

# Start all services in detached mode
docker compose up -d

# Follow application logs
docker compose logs -f app

# Stop and remove containers + volumes
docker compose down -v
```

### 3. Verify

```bash
# Health check (note: context path /8080 is required when behind reverse proxy)
curl http://localhost:8080/api/health

# Register a new user
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","email":"test@example.com","password":"SecurePass123"}'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"SecurePass123"}'
```

## Environment Variables

| Variable | Required | Description |
|----------|----------|-------------|
| `JWT_SECRET` | Yes | Secret key for signing JWT tokens |
| `ENCRYPTION_KEY` | No | AES/GCM key for OAuth2 token encryption (falls back to `JWT_SECRET`) |
| `GEMINI_API_KEY` | Yes | Google Generative AI API key |
| `YOUTUBE_CLIENT_ID` | Yes | Google OAuth2 client ID |
| `YOUTUBE_CLIENT_SECRET` | Yes | Google OAuth2 client secret |
| `DB_PASSWORD` | Yes | PostgreSQL password |
| `MINIO_ROOT_PASSWORD` | Yes | MinIO root password |
| `KAFKA_BOOTSTRAP_SERVERS` | Yes | Kafka broker addresses |
| `MINIO_ENDPOINT` | Yes | MinIO endpoint (e.g. `localhost:9000`) |
| `MINIO_ACCESS_KEY` | Yes | MinIO access key |
| `MINIO_SECRET_KEY` | Yes | MinIO secret key |
| `STORAGE_TYPE` | No | `minio` (default) or `local` |

## Project Structure

```
influencerAPP/
├── pom.xml                          # Parent POM (single module; multi-module split planned for Phase 4)
├── docker-compose.yml
├── Dockerfile
├── README.md
├── arq/
│   └── architecture.md              # Architecture specification
├── docs/
│   └── specs/
│       ├── constitution.md          # Governing principles (non-negotiable)
│       ├── PRD.md                   # Product requirements
│       ├── development-plan.md      # Phase-by-phase roadmap
│       ├── execution-state.md       # Current phase & task tracking
│       └── arq/
│           ├── architecture.md
│           ├── data-model.md
│           ├── kafka-events.md
│           └── openapi.yaml
├── src/
│   ├── main/java/com/influencerapp/
│   │   ├── InfluencerAppApplication.java
│   │   ├── domain/                  # Pure domain, zero framework deps
│   │   │   ├── model/               # Value objects & entities
│   │   │   ├── port/
│   │   │   │   ├── inbound/         # Use case interfaces
│   │   │   │   └── outbound/        # Repository & adapter interfaces
│   │   │   └── exception/           # Domain exceptions
│   │   ├── application/             # Use cases, orchestration
│   │   │   ├── service/             # Use case implementations
│   │   │   ├── dto/                 # Request/response objects
│   │   │   └── event/               # Kafka event payloads
│   │   └── infrastructure/          # Adapters, config, repos
│   │   ├── adapter/
│   │   │   ├── ai/              # Gemini text generation
│   │   │   ├── kafka/           # Kafka producer, outbox publisher, event consumer
│   │   │   ├── security/        # Token encryption
│   │   │   ├── storage/         # MinIO object storage
│   │   │   ├── youtube/         # YouTube upload
│   │   │   └── http/            # REST controllers, JWT filter
│   │       ├── config/              # Spring configuration
│   │       ├── entity/              # JPA entities
│   │   └── repository/          # Repository implementations (idempotency, outbox, processed events)
│   └── test/java/com/influencerapp/
│       ├── application/service/     # Use case unit tests
│       ├── infrastructure/adapter/  # Adapter integration & contract tests
│       └── infrastructure/repository/
└── src/main/resources/
    ├── application.yml
    ├── application-docker.yml
    └── db/migration/                # Flyway V1–V4
```

## Building and Testing

```bash
# Compile
mvn clean compile

# Run unit tests (36 tests: 22 unit tests + 9 contract tests)
mvn test

# Run integration tests (requires Docker)
mvn verify -Pintegration-tests

# Package without tests
mvn clean package -DskipTests

# Generate OpenAPI docs
mvn spring-boot:run
# Then visit http://localhost:8080/swagger-ui.html
```

### Phase 3 Verification

Run the following commands to verify Phase 3 completion:

```bash
# 1. Compile
mvn clean compile

# 2. Run unit tests (36 tests)
mvn test

# 3. Run full verification
mvn clean verify
```

**Test the endpoints** (after starting with Docker Compose):

```bash
# Health check
curl http://localhost:8080/api/health

# Register a new user
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","email":"test@example.com","password":"SecurePass123"}'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"SecurePass123"}'

# Register a YouTube channel (requires valid OAuth2 tokens)
curl -X POST http://localhost:8080/api/channels/register \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "My YouTube Channel",
    "encryptedAccessToken": "encrypted_token_here",
    "encryptedRefreshToken": "encrypted_refresh_here",
    "tokenExpiry": "2025-12-31T23:59:59Z"
  }'

# List channels
curl http://localhost:8080/api/channels \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Upload a video (multipart, with idempotency key)
curl -X POST http://localhost:8080/api/videos/upload \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Idempotency-Key: unique-upload-key-123" \
  -F "file=@/path/to/video.mp4" \
  -F "channelId=YOUR_CHANNEL_ID"

# Get video status
curl http://localhost:8080/api/videos/VIDEO_ID \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# List videos
curl "http://localhost:8080/api/videos?page=0&size=20" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

Expected results:
- `mvn clean compile` — BUILD SUCCESS
- `mvn test` — 36 tests pass (22 unit tests + 9 contract tests + 5 integration tests)
- `mvn clean verify` — BUILD SUCCESS with JAR repackaging
- All endpoints return valid JSON responses
- Video upload returns `videoId` and emits `video-received` Kafka event
- Duplicate `Idempotency-Key` returns cached response or error

## API Overview

### Implemented (Phase 2 & 3)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/auth/register` | No | Register new user |
| POST | `/api/auth/login` | No | Authenticate and receive JWT |
| POST | `/api/channels/register` | Yes | Link YouTube channel via OAuth2 |
| GET | `/api/channels` | Yes | List user channels (paginated) |
| POST | `/api/videos/upload` | Yes | Upload video (multipart, max 100MB, idempotent via `Idempotency-Key`) |
| GET | `/api/videos/{videoId}` | Yes | Get video status |
| GET | `/api/videos` | Yes | List user videos (paginated) |

All errors return RFC 7807 `application/problem+json`.

## Kafka Events (Implemented — Phase 3)

| Topic | Event | Description |
|-------|-------|-------------|
| `video-received` | `VideoReceivedEvent` | Emitted after successful upload and validation |
| `video-published` | `VideoPublishedEvent` | Emitted after YouTube upload completes |

Event payloads contain no binary data. Video references use storage paths only.

**Implemented features (Phase 3):**
- DLQ topics: `video-received-dlq`, `video-published-dlq` (after 3 retries)
- Outbox pattern: events persisted to `outbox_events` table when Kafka is unavailable
- Consumer idempotency: `processed_events` table prevents duplicate processing
- Idempotent consumers: duplicate events do not cause duplicate YouTube uploads

## Video Status Lifecycle

Videos transition through the following states during the publishing pipeline:

| Status | Description | Metadata (title/description) |
|--------|-------------|------------------------------|
| `RECEIVED` | Video uploaded successfully, outbox event created | `null` — not yet generated |
| `PROCESSING` | `video-received` event consumed, AI metadata generation in progress | `null` until Gemini responds |
| `UPLOADING` | Metadata generated, video being uploaded to YouTube | **Populated** by Gemini (or fallback) |
| `PUBLISHED` | YouTube upload completed successfully | Populated |
| `FAILED` | Error occurred during processing or upload | May be partially populated |

### Flow

1. **Upload** — `POST /api/videos/upload` creates the video with status `RECEIVED` and emits a `video-received` event to the outbox.
2. **Outbox Publishing** — `OutboxPublisher` polls the `outbox_events` table every 5 seconds and publishes pending events to Kafka.
3. **Consume** — `VideoEventConsumer` listens on `video-received` and calls `VideoProcessingService.processVideo()`.
4. **Generate Metadata** — `GenerateMetadataUseCaseImpl` invokes the Gemini adapter to generate title/description. If Gemini fails, a fallback title/description is used.
5. **Upload to YouTube** — `PublishToYouTubeUseCaseImpl` streams the video to YouTube and emits a `video-published` event.

### Checking Status

Poll `GET /api/videos/{videoId}` to track progress. The `status` field will transition from `RECEIVED` → `PROCESSING` → `UPLOADING` → `PUBLISHED`. The `metadata.title` and `metadata.description` fields are populated once the video reaches `PROCESSING`/`UPLOADING`.

## Database Migrations

Flyway manages schema evolution. Current migrations:

| Version | Description |
|---------|-------------|
| V1 | Initial schema: tenants, users, channels, videos |
| V2 | Add idempotency keys for upload deduplication |
| V3 | Add processed events table for Kafka consumer idempotency |
| V4 | Add outbox events table for reliable Kafka publishing |

## Development Phases

The project follows a phased development approach as defined in [`docs/specs/development-plan.md`](docs/specs/development-plan.md). Current status is tracked in [`docs/specs/execution-state.md`](docs/specs/execution-state.md).

| Phase | Name | Status | Description |
|-------|------|--------|-------------|
| 1 | Foundation | ✅ Complete | Project scaffolding, domain layer, ports, Flyway migrations, Docker Compose |
| 2 | Core Features | ✅ Complete | JWT auth, channel registration, Gemini AI adapter, MinIO storage, tests |
| 3 | Upload Pipeline | ✅ Complete | Video upload, YouTube publishing, Kafka event emission, outbox pattern, idempotency |
| 4 | Production Readiness | ⏳ Not Started | Monitoring, health checks, Docker production profile, CI/CD |

**Next Step:** Phase 4 — Production Readiness. Do not advance until Phase 3 is verified.

### Phase 2 Implementation Summary

**New files created:**

| File | Description |
|------|-------------|
| `src/main/java/com/influencerapp/application/service/RegisterUserUseCaseImpl.java` | User registration with BCrypt password hashing |
| `src/main/java/com/influencerapp/application/service/LoginUseCaseImpl.java` | JWT generation with `sub`, `tenantId`, `exp` claims |
| `src/main/java/com/influencerapp/infrastructure/adapter/http/JwtAuthFilter.java` | Bearer token validation, tenant extraction, RFC 7807 errors |
| `src/main/java/com/influencerapp/infrastructure/config/SecurityConfig.java` | Spring Security with stateless sessions |
| `src/main/java/com/influencerapp/infrastructure/adapter/http/AuthController.java` | REST endpoints for `/api/auth/register` and `/api/auth/login` |
| `src/main/java/com/influencerapp/infrastructure/adapter/http/ChannelController.java` | REST endpoints for `/api/channels/register` and `/api/channels` |
| `src/main/java/com/influencerapp/application/dto/UserRegistrationRequest.java` | Registration request DTO |
| `src/main/java/com/influencerapp/application/dto/LoginRequest.java` | Login request DTO |
| `src/main/java/com/influencerapp/application/dto/LoginResponse.java` | Login response DTO with JWT |
| `src/test/java/com/influencerapp/application/service/RegisterUserUseCaseImplTest.java` | 4 unit tests |
| `src/test/java/com/influencerapp/application/service/LoginUseCaseImplTest.java` | 5 unit tests |
| `src/test/java/com/influencerapp/infrastructure/adapter/ai/GeminiTextGenerationAdapterIT.java` | 2 integration tests with WireMock |
| `src/test/java/com/influencerapp/infrastructure/adapter/storage/MinioStorageAdapterIT.java` | 1 integration test with Testcontainers MinIO |
| `src/test/java/com/influencerapp/infrastructure/adapter/http/AuthControllerContractTest.java` | 4 contract tests |
| `src/test/java/com/influencerapp/infrastructure/adapter/http/ChannelControllerContractTest.java` | 4 contract tests |

**Modified files:**

| File | Change |
|------|--------|
| `GeminiTextGenerationAdapter.java` | Real Gemini API call with circuit breaker + retry + fallback |
| `MinioStorageAdapter.java` | Real MinIO streaming upload/retrieve with tenant-prefixed paths |
| `InfrastructureConfig.java` | Added `RestTemplate`, `CircuitBreaker`, `Retry`, `TimeLimiter`, `MinioClient` beans |
| `PaginatedResponse.java` | Added `@AllArgsConstructor` |
| `UserRegistrationRequest.java` | Added `@AllArgsConstructor` |
| `PageResult.java` | Added explicit getters |
| `pom.xml` | Added `resilience4j-retry` and `spring-cloud-contract-wiremock` dependencies |

## Clean Code & Lombok

This project follows Clean Code principles and uses Lombok to eliminate boilerplate.

- **Lombok annotations**: `@Value`, `@Data`, `@Builder`, `@Slf4j`, `@RequiredArgsConstructor`, `@With`, `@AllArgsConstructor`, `@NoArgsConstructor(force = true)` for JPA entities.
- **No manual getters/setters/constructors** unless Lombok cannot express the invariant.
- **Single responsibility**: one class, one reason to change.
- **Small functions**: methods <= 30 lines, classes <= 300 lines.
- **Immutability**: domain models and value objects are immutable.
- **No dead code**: no commented-out code, no unused imports.
- **Logging**: `@Slf4j` only; no `System.out` or `System.err`.

## Contributing

1. Read `docs/specs/constitution.md` — these are non-negotiable rules.
2. Read `docs/specs/development-plan.md` and `docs/specs/execution-state.md` to understand the current phase and pending tasks.
3. Read `docs/specs/arq/architecture.md` before implementing any new adapter or use case.
4. Follow the Maven structure (`domain`, `application`, `infrastructure` packages; multi-module split planned for Phase 3).
5. Enforce layer boundaries: `domain` must never import Spring, JPA, Kafka, or SDKs.
6. Write unit tests for all use cases (90%+ coverage target).
7. Write integration tests for all adapters using Testcontainers.
8. Ensure all Kafka events use kebab-case topic names and include `tenantId`.
9. Never load full video files into memory; always stream.
10. Never hardcode secrets; use environment variables.
11. Update `docs/specs/execution-state.md` before and after making changes.
12. Submit PRs with clear descriptions linked to user stories or constitution amendments.

## License

Proprietary — all rights reserved.
