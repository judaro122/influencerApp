# InfluencerAPP

Backend service that automates YouTube content publishing. Users upload videos through a REST API; the system generates AI-powered titles and descriptions using Google Gemini (free tier), uploads the video to a pre-registered YouTube channel, and emits lifecycle events through Apache Kafka. Multi-tenancy is enforced by default: each authenticated user manages their own YouTube channels.

## Key Features

- **Hexagonal Architecture**: strict ports & adapters with zero framework dependencies in the domain layer.
- **Multi-Tenancy**: tenant isolation in every query, storage path, and Kafka payload.
- **Streaming Uploads**: video files are never fully loaded into memory; chunked transfer to MinIO and YouTube.
- **AI Metadata**: Gemini-powered title and description generation with circuit breaker and placeholder fallback.
- **Event-Driven**: asynchronous upload pipeline via Kafka with DLQ, outbox pattern, and consumer idempotency.
- **Resilience**: Resilience4j circuit breakers, exponential backoff with jitter, graceful degradation.
- **Security**: OAuth2 YouTube linking, AES/GCM/NoPadding token encryption.
- **Database Migrations**: Flyway-managed schema evolution (V1–V4).
- **Observability**: Micrometer + Prometheus metrics, health checks for all dependencies.
- **Idempotency**: upload requests protected by `Idempotency-Key` header; processed events tracked to prevent duplicates.

## Tech Stack

| Concern | Technology |
|---------|-----------|
| Language | Java 17 |
| Framework | Spring Boot 3.3.4 |
| Architecture | Hexagonal (Ports & Adapters) |
| Build | Maven (multi-module) |
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
mvn clean verify
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
curl http://localhost:8080/api/health
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
├── pom.xml                          # Parent POM
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
│       └── execution-state.md       # Current phase & task tracking
├── domain/                          # Pure domain, zero framework deps
│   ├── model/                       # Value objects & entities
│   ├── port/
│   │   ├── inbound/                 # Use case interfaces
│   │   └── outbound/                # Repository & adapter interfaces
│   └── exception/                   # Domain exceptions
├── application/                     # Use cases, orchestration
│   ├── service/                     # Use case implementations
│   ├── dto/                         # Request/response objects
│   └── event/                       # Kafka event payloads
└── infrastructure/                  # Adapters, config, repos
    ├── adapter/
    │   ├── ai/                      # Gemini text generation
    │   ├── kafka/                   # Kafka producer
    │   ├── security/                # Token encryption
    │   ├── storage/                 # MinIO object storage
    │   └── youtube/                 # YouTube upload
    ├── config/                      # Spring configuration
    ├── entity/                      # JPA entities
    └── repository/                  # Repository implementations
```

## Building and Testing

```bash
# Compile all modules
mvn compile

# Run unit tests
mvn test

# Run integration tests (requires Docker)
mvn verify -Pintegration-tests

# Package without tests
mvn clean package -DskipTests

# Generate OpenAPI docs
mvn spring-boot:run -pl infrastructure
# Then visit http://localhost:8080/swagger-ui.html
```

## API Overview

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/auth/register` | No | Register new user |
| POST | `/api/auth/login` | No | Authenticate and receive JWT |
| GET | `/api/health` | No | Health check |
| POST | `/api/channels/register` | Yes | Link YouTube channel via OAuth2 |
| GET | `/api/channels` | Yes | List user channels (paginated) |
| POST | `/api/videos/upload` | Yes | Upload video (multipart, max 100MB, idempotent) |
| GET | `/api/videos/{videoId}` | Yes | Get video status |
| GET | `/api/videos` | Yes | List user videos (paginated) |

All errors return RFC 7807 `application/problem+json`.

## Kafka Events

| Topic | Event | Description |
|-------|-------|-------------|
| `video-received` | `VideoReceivedEvent` | Emitted after successful upload and validation |
| `video-published` | `VideoPublishedEvent` | Emitted after YouTube upload completes |

Event payloads contain no binary data. Video references use storage paths only.

## Database Migrations

Flyway manages schema evolution. Current migrations:

| Version | Description |
|---------|-------------|
| V1 | Initial schema: tenants, users, channels, videos |
| V2 | Add idempotency keys for upload deduplication |
| V3 | Add processed events table for Kafka consumer idempotency |
| V4 | Add outbox events table for reliable Kafka publishing |

## Clean Code & Lombok

This project follows Clean Code principles and uses Lombok to eliminate boilerplate.

- **Lombok annotations**: `@Value`, `@Data`, `@Builder`, `@Slf4j`, `@RequiredArgsConstructor`, `@With`, `@NoArgsConstructor(force = true)` for JPA entities.
- **No manual getters/setters/constructors** unless Lombok cannot express the invariant.
- **Single responsibility**: one class, one reason to change.
- **Small functions**: methods <= 30 lines, classes <= 300 lines.
- **Immutability**: domain models and value objects are immutable.
- **No dead code**: no commented-out code, no unused imports.
- **Logging**: `@Slf4j` only; no `System.out` or `System.err`.

## Contributing

1. Read `docs/specs/constitution.md` — these are non-negotiable rules.
2. Read `arq/architecture.md` before implementing any new adapter or use case.
3. Follow the multi-module Maven structure (`domain`, `application`, `infrastructure`).
4. Enforce layer boundaries: `domain` must never import Spring, JPA, Kafka, or SDKs.
5. Write unit tests for all use cases (90%+ coverage target).
6. Write integration tests for all adapters using Testcontainers.
7. Ensure all Kafka events use kebab-case topic names and include `tenantId`.
8. Never load full video files into memory; always stream.
9. Never hardcode secrets; use environment variables.
10. Submit PRs with clear descriptions linked to user stories or constitution amendments.

## License

Proprietary — all rights reserved.
