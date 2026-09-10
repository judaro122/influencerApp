# Execution State

## Current Phase
**Phase 3: Upload Pipeline** - COMPLETED

## Last Updated
2026-08-25

## Bug Fix: Foreign Key Constraint on User Registration

### Issue
`POST /api/auth/register` failed with:
```
ERROR: insert or update on table "users" violates foreign key constraint "users_tenant_id_fkey"
Detail: Key (tenant_id)=(...) is not present in table "tenants".
```

### Root Cause
`RegisterUserUseCaseImpl` generated a random `tenantId` for each new user but never created the corresponding `Tenant` record in the `tenants` table. The `users` table has a foreign key constraint `users_tenant_id_fkey` referencing `tenants(id)`, causing the insert to fail.

### Fix Applied
- Created `TenantRepository` port in `domain/port/outbound`
- Created `JpaTenantRepository` and `TenantRepositoryImpl` in `infrastructure/repository`
- Updated `RegisterUserUseCaseImpl` to create a `Tenant` record before creating the `User`
- Updated `InfrastructureConfig` to wire the `TenantRepository` bean
- Updated unit test `RegisterUserUseCaseImplTest` to mock the new `TenantRepository`
- Fixed pre-existing test assertion in `AuthControllerContractTest`

### Modified Files (Bug Fix)
- `src/main/java/com/influencerapp/domain/port/outbound/TenantRepository.java` (new)
- `src/main/java/com/influencerapp/infrastructure/repository/JpaTenantRepository.java` (new)
- `src/main/java/com/influencerapp/infrastructure/repository/TenantRepositoryImpl.java` (new)
- `src/main/java/com/influencerapp/application/service/RegisterUserUseCaseImpl.java`
- `src/main/java/com/influencerapp/infrastructure/config/InfrastructureConfig.java`
- `src/test/java/com/influencerapp/application/service/RegisterUserUseCaseImplTest.java`
- `src/test/java/com/influencerapp/infrastructure/adapter/http/AuthControllerContractTest.java`
## Bug Fix: Channel Registration DTO Mismatch

### Issue
`POST /api/channels/register` returned 400 Bad Request because the request body fields didn't match the API's expected format. The API expected `authCode` but the client was sending `name`, `encryptedAccessToken`, `encryptedRefreshToken`, `tokenExpiry`.

### Fix Applied
- Updated `ChannelRegistrationRequest` DTO to include `name`, `encryptedAccessToken`, `encryptedRefreshToken`, `tokenExpiry`
- Updated `RegisterChannelUseCase` port signature
- Updated `RegisterChannelUseCaseImpl` to accept the new parameters directly
- Updated `ChannelController` to pass new fields to use case
- Updated `ChannelControllerContractTest` to use new DTO fields

### Modified Files
- `src/main/java/com/influencerapp/application/dto/ChannelRegistrationRequest.java`
- `src/main/java/com/influencerapp/domain/port/inbound/RegisterChannelUseCase.java`
- `src/main/java/com/influencerapp/application/service/RegisterChannelUseCaseImpl.java`
- `src/main/java/com/influencerapp/infrastructure/adapter/http/ChannelController.java`
- `src/test/java/com/influencerapp/application/service/RegisterChannelUseCaseImplTest.java`
- `src/test/java/com/influencerapp/infrastructure/adapter/http/ChannelControllerContractTest.java`

## Bug Fix: Channel Registration Foreign Key Constraint

### Issue
`POST /api/channels/register` failed with:
```
ERROR: insert or update on table "channels" violates foreign key constraint "channels_tenant_id_fkey"
Detail: Key (tenant_id)=(...) is not present in table "tenants".
```

### Root Cause
`ChannelController.extractTenantId()` was generating a random UUID instead of extracting the `tenantId` from the JWT token set by `JwtAuthFilter`.

### Fix Applied
- Updated `ChannelController.extractTenantId()` to read `tenantId` from request context set by `JwtAuthFilter`
- Updated contract tests to set `tenantId` request attribute manually (since filters are disabled in `@WebMvcTest`)

### Modified Files
- `src/main/java/com/influencerapp/infrastructure/adapter/http/ChannelController.java`
- `src/test/java/com/influencerapp/infrastructure/adapter/http/ChannelControllerContractTest.java`

## Investigation: Login "Invalid credentials"

### Issue
`POST /api/auth/login` returns "Invalid credentials" error.

### Analysis
The `LoginUseCaseImpl` code is correct:
1. Validates email and password are not blank
2. Looks up user by email via `userRepository.findByEmail(email)`
3. If not found, throws "Invalid credentials"
4. If password doesn't match BCrypt hash, throws "Invalid credentials"
5. If valid, generates JWT with `sub`, `tenantId`, `exp` claims

The error is **expected behavior** — the user doesn't exist in the database because:
- Registration was previously failing due to the foreign key constraint
- No users were actually created in the database
- The user needs to register first (which now works after the fix), then login

### Resolution
1. Register a new user with `POST /api/auth/register`
2. Then login with `POST /api/auth/login` using the same credentials

### Verification
All 14 relevant tests pass:
- `LoginUseCaseImplTest` - 5 tests
- `RegisterUserUseCaseImplTest` - 4 tests
- `AuthControllerContractTest` - 5 tests

## Phase 1 Completion Status

### 1.1 Project Scaffolding
- [x] Maven multi-module structure (single module for now, will split later)
- [x] Parent POM with dependency management
- [x] Spring Boot 3.3.4 configuration
- [x] Lombok configuration
- [x] JJWT 0.12.5 for JWT handling

### 1.2 Domain Layer
- [x] All domain models with `@Value` and `@AllArgsConstructor`
- [x] All port interfaces with method signatures
- [x] Domain exceptions
- [x] Zero framework dependencies in domain

### 1.3 Application Layer
- [x] All use case implementations
- [x] DTOs for requests/responses
- [x] Kafka event classes

### 1.4 Infrastructure Layer
- [x] JPA entities (Video, Channel, User, Tenant)
- [x] JPA repository interfaces
- [x] Repository implementations
- [x] Infrastructure configuration
- [x] JWT authentication filter
- [x] Security configuration
- [x] Token encryption service
- [x] Stub adapters (MinIO, YouTube, Gemini, Kafka)

### 1.5 Tests (New Strategy - No Hyper-Granular Unit Tests)
- [x] Use case tests with mocked ports (5 test classes, 12 tests)
- [x] Integration tests for repository adapters with Testcontainers (3 test classes, tagged with `@Tag("integration")`)
- [x] No tests for value objects, DTOs, or mappers
- [x] All tests pass with `mvn clean test`

## Phase 2 Completion Status

### 2.1 User Registration & JWT Authentication
- [x] `RegisterUserUseCaseImpl` - hashes password with BCrypt, creates tenant, saves user
- [x] `LoginUseCaseImpl` - validates credentials, generates JWT with `sub`, `tenantId`, `exp` claims using JJWT
- [x] `JwtAuthFilter` - validates Bearer tokens, extracts tenantId, sets SecurityContext, returns RFC 7807 errors
- [x] `SecurityConfig` - configures Spring Security with stateless sessions, permits `/api/auth/**` and `/api/health`, adds JWT filter
- [x] `AuthController` - REST endpoints for `/api/auth/register` and `/api/auth/login`
- [x] DTOs: `UserRegistrationRequest`, `LoginRequest`, `LoginResponse`

### 2.2 Channel Registration with OAuth2 Token Encryption
- [x] `RegisterChannelUseCaseImpl` - already existed from Phase 1
- [x] `ChannelController` - REST endpoint for `/api/channels/register` and `/api/channels`
- [x] DTOs: `ChannelRegistrationRequest`, `ChannelResponse`, `PaginatedResponse`
- [x] `TokenEncryptionService` - AES/GCM/NoPadding encryption for OAuth2 tokens

### 2.3 Gemini AI Adapter with Circuit Breaker
- [x] `GeminiTextGenerationAdapter` - real HTTP call to Gemini API with Resilience4j circuit breaker + retry + fallback
- [x] `GenerateMetadataUseCaseImpl` - already existed from Phase 1
- [x] Infrastructure config: `RestTemplate`, `CircuitBreaker`, `Retry`, `TimeLimiter` beans
- [x] Added `resilience4j-retry` dependency to `pom.xml`

### 2.4 MinIO Storage Adapter with Streaming
- [x] `MinioStorageAdapter` - real streaming multipart upload to MinIO with tenant-prefixed paths
- [x] `InfrastructureConfig` - `MinioClient` bean configuration
- [x] No full video file loads into memory

### 2.5 Unit Tests
- [x] `RegisterUserUseCaseImplTest` - 4 tests
- [x] `LoginUseCaseImplTest` - 5 tests
- [x] All existing use case tests still pass

### 2.6 Integration Tests
- [x] `GeminiTextGenerationAdapterIT` - 2 tests with WireMock
- [x] `MinioStorageAdapterIT` - 1 test with Testcontainers MinIO

### 2.7 Contract Tests
- [x] `AuthControllerContractTest` - 5 tests
- [x] `ChannelControllerContractTest` - 4 tests

## Modified Files

### New Files (Phase 2)
- `src/main/java/com/influencerapp/application/service/RegisterUserUseCaseImpl.java`
- `src/main/java/com/influencerapp/application/service/LoginUseCaseImpl.java`
- `src/main/java/com/influencerapp/infrastructure/adapter/http/JwtAuthFilter.java`
- `src/main/java/com/influencerapp/infrastructure/config/SecurityConfig.java`
- `src/main/java/com/influencerapp/infrastructure/adapter/http/AuthController.java`
- `src/main/java/com/influencerapp/infrastructure/adapter/http/ChannelController.java`
- `src/main/java/com/influencerapp/application/dto/UserRegistrationRequest.java`
- `src/main/java/com/influencerapp/application/dto/LoginRequest.java`
- `src/main/java/com/influencerapp/application/dto/LoginResponse.java`
- `src/test/java/com/influencerapp/application/service/RegisterUserUseCaseImplTest.java`
- `src/test/java/com/influencerapp/application/service/LoginUseCaseImplTest.java`
- `src/test/java/com/influencerapp/infrastructure/adapter/ai/GeminiTextGenerationAdapterIT.java`
- `src/test/java/com/influencerapp/infrastructure/adapter/storage/MinioStorageAdapterIT.java`
- `src/test/java/com/influencerapp/infrastructure/adapter/http/AuthControllerContractTest.java`
- `src/test/java/com/influencerapp/infrastructure/adapter/http/ChannelControllerContractTest.java`

### Modified Files (Phase 2)
- `src/main/java/com/influencerapp/infrastructure/adapter/ai/GeminiTextGenerationAdapter.java` - added real Gemini API call with circuit breaker + retry + fallback
- `src/main/java/com/influencerapp/infrastructure/adapter/storage/MinioStorageAdapter.java` - added real MinIO streaming upload/retrieve
- `src/main/java/com/influencerapp/infrastructure/config/InfrastructureConfig.java` - added `RestTemplate`, `CircuitBreaker`, `Retry`, `TimeLimiter`, `MinioClient` beans
- `src/main/java/com/influencerapp/application/dto/PaginatedResponse.java` - added `@AllArgsConstructor`
- `src/main/java/com/influencerapp/application/dto/UserRegistrationRequest.java` - added `@AllArgsConstructor`
- `src/main/java/com/influencerapp/domain/model/PageResult.java` - added explicit getters
- `pom.xml` - added `resilience4j-retry` and `spring-cloud-contract-wiremock` dependencies

## Phase 1 Completion Status (Updated)
- [x] JWT authentication filter
- [x] Security configuration
- [x] All tests pass with `mvn clean test`

## Next Steps
- **STOP** - Do not advance to Phase 3 until Phase 2 is verified
- Run verification steps below to confirm Phase 2 completion
- If verified, proceed to Phase 3: Kafka Integration & Event-Driven Pipeline

## Phase 3 Completion Status

### 3.1 Upload Pipeline with Idempotency
- [x] `UploadVideoUseCaseImpl` - validates file type/size, stores via MinIO, saves video, emits `video-received` event
- [x] `IdempotencyKeyRepository` - checks for duplicate uploads via `Idempotency-Key` header
- [x] `OutboxEventRepository` - persists events to `outbox_events` table for reliable Kafka publishing
- [x] `VideoEventProducer` - publishes `video-received` events to Kafka topic `video-received`
- [x] `OutboxPublisher` - scheduled task that polls `outbox_events` and publishes to Kafka

### 3.2 YouTube Publishing
- [x] `PublishToYouTubeUseCaseImpl` - downloads video stream from MinIO, uploads to YouTube, emits `video-published` event
- [x] `YouTubeUploadAdapter` - streaming upload (no `byte[]` full load), token refresh support
- [x] `VideoProcessingService` - orchestrates video processing pipeline

### 3.3 Kafka Event Consumption with Idempotency
- [x] `VideoEventConsumer` - consumes `video-received` events with idempotency via `processed_events` table
- [x] DLQ routing - after 3 failures, events routed to `video-received-dlq` topic
- [x] Consumer idempotency - duplicate events skipped via `processed_events` unique constraint

### 3.4 Video Controller & List Endpoints
- [x] `VideoController` - `POST /api/videos/upload`, `GET /api/videos/{videoId}`, `GET /api/videos`
- [x] `ListVideosUseCaseImpl` - paginated video listing with tenant isolation
- [x] `VideoStatusResponse` - response DTO with video status, metadata, YouTube URL

### 3.5 Database Schema
- [x] `idempotency_keys` table - stores idempotency keys with 24h TTL
- [x] `outbox_events` table - stores pending Kafka events
- [x] `processed_events` table - tracks processed event IDs for consumer idempotency

### 3.6 Unit Tests
- [x] `UploadVideoUseCaseImplTest` - 4 tests (success, duplicate, invalid type, invalid size)
- [x] `PublishToYouTubeUseCaseImplTest` - 2 tests (success, failure)
- [x] `ListVideosUseCaseImplTest` - already existed from Phase 1

### 3.7 Contract Tests
- [x] `VideoControllerContractTest` - 3 tests (upload, get status, list)

## Modified Files

### New Files (Phase 3)
- `src/main/java/com/influencerapp/domain/port/outbound/IdempotencyKeyRepository.java`
- `src/main/java/com/influencerapp/domain/port/outbound/OutboxEventRepository.java`
- `src/main/java/com/influencerapp/domain/port/outbound/ProcessedEventRepository.java`
- `src/main/java/com/influencerapp/domain/port/inbound/PublishToYouTubeUseCase.java`
- `src/main/java/com/influencerapp/domain/port/inbound/ListVideosUseCase.java`
- `src/main/java/com/influencerapp/domain/model/OutboxEvent.java`
- `src/main/java/com/influencerapp/infrastructure/entity/IdempotencyKeyEntity.java`
- `src/main/java/com/influencerapp/infrastructure/entity/OutboxEventEntity.java`
- `src/main/java/com/influencerapp/infrastructure/entity/ProcessedEventEntity.java`
- `src/main/java/com/influencerapp/infrastructure/repository/JpaIdempotencyKeyRepository.java`
- `src/main/java/com/influencerapp/infrastructure/repository/JpaOutboxEventRepository.java`
- `src/main/java/com/influencerapp/infrastructure/repository/JpaProcessedEventRepository.java`
- `src/main/java/com/influencerapp/infrastructure/repository/IdempotencyKeyRepositoryImpl.java`
- `src/main/java/com/influencerapp/infrastructure/repository/OutboxEventRepositoryImpl.java`
- `src/main/java/com/influencerapp/infrastructure/repository/ProcessedEventRepositoryImpl.java`
- `src/main/java/com/influencerapp/application/service/VideoProcessingService.java`
- `src/main/java/com/influencerapp/application/service/PublishToYouTubeUseCaseImpl.java`
- `src/main/java/com/influencerapp/application/service/ListVideosUseCaseImpl.java`
- `src/main/java/com/influencerapp/infrastructure/adapter/kafka/VideoEventProducer.java`
- `src/main/java/com/influencerapp/infrastructure/adapter/kafka/OutboxPublisher.java`
- `src/main/java/com/influencerapp/infrastructure/adapter/kafka/VideoEventConsumer.java`
- `src/main/java/com/influencerapp/infrastructure/adapter/http/VideoController.java`
- `src/test/java/com/influencerapp/application/service/UploadVideoUseCaseImplTest.java`
- `src/test/java/com/influencerapp/application/service/PublishToYouTubeUseCaseImplTest.java`
- `src/test/java/com/influencerapp/infrastructure/adapter/http/VideoControllerContractTest.java`

### Modified Files (Phase 3)
- `src/main/java/com/influencerapp/application/service/UploadVideoUseCaseImpl.java` - added idempotency, outbox event emission
- `src/main/java/com/influencerapp/infrastructure/adapter/youtube/YouTubeUploadAdapter.java` - removed `byte[]` overload, fixed token refresh
- `src/main/java/com/influencerapp/infrastructure/config/InfrastructureConfig.java` - added new repository beans, VideoEventProducer, OutboxPublisher
- `pom.xml` - added `spring-security-test` dependency

## Next Steps
- **STOP** - Do not advance to Phase 4 until Phase 3 is verified
- Run verification steps below to confirm Phase 3 completion
- If verified, proceed to Phase 4: Observability & Production Readiness

## Verification Steps for Phase 3

Run the following commands to verify Phase 3 completion:

```bash
# 1. Compile all modules
mvn clean compile

# 2. Run unit tests (36 tests)
mvn test

# 3. Run full verification
mvn clean verify
```

Expected results:
- `mvn clean compile` - BUILD SUCCESS
- `mvn test` - 36 tests pass (22 unit tests + 9 contract tests + 5 integration tests)
- `mvn clean verify` - BUILD SUCCESS with JAR repackaging

## Bug Fix: tenant_id Column Type Mismatch (UUID vs VARCHAR)

### Issue
Application failed to start with:
```
Schema-validation: wrong column type encountered in column [tenant_id] in table [idempotency_keys];
found [uuid (Types#OTHER)], but expecting [varchar(255) (Types#VARCHAR)]
```

### Root Cause
Migrations V2, V3, and V4 defined `tenant_id` as `UUID` type, but:
- Migration V1 defines all `tenant_id` columns as `VARCHAR(255)`
- All JPA entities (`IdempotencyKeyEntity`, `ProcessedEventEntity`, `OutboxEventEntity`) map `tenantId` as `String` with `length = 255`
- The `TenantId` domain model uses `String value`
- `hibernate.ddl-auto: validate` enforces schema-entity consistency at startup

The `UUID` type in V2-V4 was inconsistent with the rest of the codebase, causing Hibernate schema validation to fail.

### Fix Applied
- Fixed `V2__add_idempotency_keys.sql`: changed `tenant_id UUID` → `tenant_id VARCHAR(255)`
- Fixed `V3__add_processed_events.sql`: changed `tenant_id UUID` → `tenant_id VARCHAR(255)`
- Fixed `V4__add_outbox_events.sql`: changed `tenant_id UUID` → `tenant_id VARCHAR(255)`
- Created `V5__fix_tenant_id_column_types.sql`: idempotent migration that ALTERs any remaining `UUID` columns to `VARCHAR(255)` using `USING tenant_id::text` (for existing databases that already applied V2-V4 with the UUID type)

### Modified Files
- `src/main/resources/db/migration/V2__add_idempotency_keys.sql` (modified)
- `src/main/resources/db/migration/V3__add_processed_events.sql` (modified)
- `src/main/resources/db/migration/V4__add_outbox_events.sql` (modified)
- `src/main/resources/db/migration/V5__fix_tenant_id_column_types.sql` (new)

### Post-Fix Action Required
Since V2-V4 were modified (checksum change), existing databases need to be recreated:
```bash
docker compose down -v  # removes postgres-data volume
docker compose up -d
```
On a fresh database, V1-V5 apply cleanly. V5 is a no-op on fresh databases (columns already VARCHAR(255) from corrected V2-V4).
