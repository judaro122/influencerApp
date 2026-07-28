# InfluencerAPP — Integrations Specification

## 1. Overview

This document defines the resilience patterns, fallback strategies, and streaming behavior for external integrations (Gemini, YouTube, MinIO) in the InfluencerAPP platform. It is derived from the [Project Constitution](../constitution.md), the [Architecture Specification](../architecture.md), the [Data Model Specification](data-model.md), and the [Security Specification](security-spec.md).

---

## 2. General Resilience Configuration

### 2.1 Retry Policy (All External Calls)

Applied uniformly to Gemini, YouTube, and MinIO integrations per constitution §7.1:

| Parameter | Value |
|-----------|-------|
| Initial interval | 1 second |
| Max interval | 30 seconds |
| Multiplier | 2.0 (exponential) |
| Max attempts | 3 |
| Jitter | ±10% |

### 2.2 Circuit Breaker (Resilience4j)

| Parameter | Value |
|-----------|-------|
| Failure threshold | 50% of calls over 10 seconds |
| Wait duration | 60 seconds before transitioning to HALF_OPEN |
| Minimum calls in rolling window | 10 |
| Permitted calls in HALF_OPEN | 3 |
| State transition | CLOSED → OPEN → HALF_OPEN → CLOSED |

### 2.3 Failure Behavior Matrix

| Integration | Failure | Behavior |
|-------------|---------|----------|
| Gemini | Circuit breaker OPEN | Fallback to placeholder text; upload proceeds |
| Gemini | Rate limit reached (429) | Circuit breaker triggers; exponential backoff retry |
| Gemini | Max retries exhausted | Circuit breaker OPEN; fallback placeholder for all subsequent calls |
| YouTube | Circuit breaker OPEN | Mark video as FAILED; emit error event to Kafka DLQ |
| YouTube | Upload failure (network) | Retry up to 3 times with exponential backoff |
| YouTube | Max retries exhausted | Mark video as FAILED; emit error event to Kafka DLQ |
| YouTube | Quota exhausted | Circuit breaker OPEN; notify via metric |
| MinIO | Storage unavailable (prod) | Return HTTP 503; do not proceed with upload |
| MinIO | Storage unavailable (`storage.type=local`) | Fallback to local filesystem |
| MinIO | Upload failure (network) | Retry up to 3 times with exponential backoff |
| Kafka | Broker unavailable | Persist event to `outbox_events`; retry on reconnect |
| Kafka | Out of retries | Event remains in outbox; DLQ for manual inspection |

---

## 3. Gemini AI Integration

### 3.1 Adapter

`GeminiAdapter` implements `AITextGenerationPort`.

| Property | Value |
|----------|-------|
| Model | `gemini-2.0-flash` (free tier compatible) |
| API authentication | API Key via environment variable |
| SDK | Google Generative AI Java SDK |

### 3.2 Resilience4j Circuit Breaker Configuration

```
CircuitBreakerConfig.custom()
  .failureRateThreshold(50)
  .waitDurationInOpenState(Duration.ofSeconds(60))
  .slidingWindowSize(10)
  .minimumNumberOfCalls(10)
  .permittedNumberOfCallsInHalfOpenState(3)
  .slowCallRateThreshold(50)
  .slowCallDurationThreshold(Duration.ofSeconds(5))
  .build()
```

### 3.3 Retry Configuration

```
RetryConfig.custom()
  .initialInterval(Duration.ofSeconds(1))
  .maxInterval(Duration.ofSeconds(30))
  .multiplier(2.0)
  .maxAttempts(3)
  .jitter(0.1)
  .retryOnException(e -> e instanceof GeminiException)
  .ignoreExceptions(e -> e instanceof InvalidRequestException)
  .build()
```

### 3.4 Fallback Logic

The fallback is invoked when the circuit breaker is OPEN or when all retry attempts are exhausted per constitution §6 and architecture §11.4.

**Fallback input:** Original `FileMetadata` (filename, size, mimeType).

**Fallback behavior:**

| Field | Value |
|-------|-------|
| `title` | `"Title for {filename}"` |
| `description` | `"Description for {filename}"` |

The upload proceeds with placeholder metadata. The fallback is logged at WARN level but is non-fatal (constitution §6, architecture §11.3).

### 3.5 Rate Limit Handling

- Free tier quotas are architectural constraints, not operational afterthoughts (constitution §2.6).
- The circuit breaker opens at 50% failure rate over a 10-second window, preventing quota exhaustion from impacting other tenants.
- `gemini.latency` metric is emitted per request for monitoring (architecture §14.2).

### 3.6 Implementation Requirements

| Requirement | Detail |
|-------------|--------|
| Never load entire file into memory | Pass `InputStream` reference; Gemini SDK must receive streaming input where possible |
| Timeout | HTTP connection/read timeout ≤ 10 seconds |
| API key | Injected via environment variable; never hardcoded |
| Logging | Log request latency and response status; never log the API key or generated content contents |

---

## 4. YouTube Integration

### 4.1 Adapter

`YouTubeUploadAdapter` implements `YouTubeUploadPort`.

| Property | Value |
|----------|-------|
| API | YouTube Data API v3 |
| Authentication | OAuth2 with per-user tokens (encrypted at rest) |
| Scopes | `https://www.googleapis.com/auth/youtube.upload` only (constitution §4.4) |
| Upload method | Resumable upload via YouTube Data API |

### 4.2 Token Refresh

`YouTubeUploadAdapter` transparently refreshes `access_token` before every upload operation (architecture §11.2):

1. Check `token_expiry`; if expired or expiring within 60 seconds, proceed to refresh.
2. Call Google token endpoint with `refresh_token`.
3. Decrypt stored `refresh_token_enc` using AES/GCM (security spec §4).
4. Use the decrypted refresh token to obtain a new `access_token`.
5. Re-encrypt the new `access_token` before storing it back in the database.
6. The `refresh_token` itself is never exposed to the client after initial registration.

### 4.3 Resilience4j Circuit Breaker Configuration

```
CircuitBreakerConfig.custom()
  .failureRateThreshold(50)
  .waitDurationInOpenState(Duration.ofSeconds(60))
  .slidingWindowSize(10)
  .minimumNumberOfCalls(10)
  .permittedNumberOfCallsInHalfOpenState(3)
  .slowCallRateThreshold(50)
  .slowCallDurationThreshold(Duration.ofSeconds(10))
  .build()
```

### 4.4 Retry Configuration

```
RetryConfig.custom()
  .initialInterval(Duration.ofSeconds(1))
  .maxInterval(Duration.ofSeconds(30))
  .multiplier(2.0)
  .maxAttempts(3)
  .jitter(0.1)
  .retryOnException(e -> e instanceof YouTubeApiException)
  .ignoreExceptions(e -> e instanceof InvalidTokenException)
  .build()
```

### 4.5 Fallback Logic

Unlike Gemini, YouTube failures are fatal to the individual video — there is no placeholder for a failed upload.

| Failure Scenario | Behavior |
|------------------|----------|
| Circuit breaker OPEN | Mark video as FAILED; emit `VideoPublishedEvent` with error details |
| Upload retry exhausted | Mark video as FAILED; emit `VideoPublishedEvent` with error details |
| Invalid token (401) | Do NOT retry; mark video as FAILED; log credential issue for investigation |
| Quota exceeded | Circuit breaker opens; metric `youtube.quota.used` emitted; all subsequent uploads blocked until quota resets |

### 4.6 Upload Execution

1. Retrieve video from object storage as `InputStream` (streaming, never full `byte[]` load).
2. Initiate resumable upload session with YouTube Data API.
3. Stream file content in chunks to YouTube.
4. No synchronous wait for publish completion — the upload is fire-and-forget (architecture §11.4).
5. If upload completes successfully, transition video to `PUBLISHED` and emit Kafka event.
6. On failure, retry up to 3 times with exponential backoff; then mark `FAILED` and emit error event.

### 4.7 Implementation Requirements

| Requirement | Detail |
|-------------|--------|
| Streaming only | Video data is streamed from MinIO/Local storage to YouTube; never fully buffered in memory |
| Encrypted tokens | `access_token` stored encrypted; decryption in-memory only for the upload duration |
| Token refresh | Transparent; no client intervention required |
| Timeout | Connection timeout ≤ 10s; read timeout ≤ 300s (YouTube processing may be slow) |
| No additional scopes | Only `youtube.upload` scope requested; no additional scopes without explicit approval (constitution §4.4) |

---

## 5. MinIO Object Storage Integration

### 5.1 Adapter

`MinIOStorageAdapter` implements `ObjectStoragePort` (constitution §3.2).

| Property | Value |
|----------|-------|
| Protocol | S3-compatible API |
| Endpoint | Configured via `spring.minio.endpoint` |
| Bucket strategy | Single bucket with tenant-prefixed paths, or bucket-per-tenant `tenant-{tenantId}` |
| Authentication | Access Key / Secret Key from environment variables |

### 5.2 Streaming Multipart Upload

Video uploads to MinIO MUST use streaming/chunked transfer; the entire file MUST NEVER be loaded into application memory (constitution §2.3, architecture §9.1).

#### Upload Path

```
InputStream (from HTTP multipart request)
  │
  ▼
┌──────────────────────────────┐
│  MinIOStorageAdapter         │
│  1. Validate file metadata   │
│  2. Generate storage key     │
│     tenants/{tenantId}/      │
│     videos/{videoId}/        │
│     {filename}               │
│  3. Initiate multipart upload│
│  4. Stream chunks to MinIO   │
│     (chunk size configurable)│
│  5. Complete multipart       │
│  6. Return StoragePath       │
└──────────────────────────────┘
```

#### Key Configuration Parameters

| Parameter | Default | Description |
|-----------|---------|-------------|
| `minio.chunk-size` | 5MB | Size of each multipart chunk |
| `minio.max-file-size` | 100MB | Maximum file size; configurable via `spring.servlet.multipart.max-file-size` |
| `minio.connect-timeout` | 10s | Connection timeout |
| `minio.read-timeout` | 30s | Read/write timeout per chunk |

#### Multipart Upload Algorithm

1. Receive `InputStream` from the HTTP multipart request.
2. Read chunks from the `InputStream` into a buffer (configurable chunk size; default 5MB).
3. Upload each chunk via the MinIO SDK multipart upload API.
4. After all chunks are uploaded, complete the multipart upload.
5. The buffer for each chunk is a small fixed-size byte array (e.g., 5MB); it is discarded after the chunk is uploaded and the next chunk is read.
6. **No `byte[]` or `byte[][]` that contains the full file is ever allocated.**

#### Download Path (Streaming)

```
MinIO key
  │
  ▼
┌──────────────────────────────┐
│  MinIOStorageAdapter         │
│  1. Initiate GET request     │
│  2. Read response as stream  │
│  3. Stream to YouTube upload │
│     or to HTTP response      │
│  4. Close connection         │
└──────────────────────────────┘
```

For YouTube upload (architecture §11.4): the adapter streams video data directly from MinIO to the YouTube upload endpoint without materializing the full file in memory.

### 5.3 Resilience4j Configuration

```
CircuitBreakerConfig.custom()
  .failureRateThreshold(50)
  .waitDurationInOpenState(Duration.ofSeconds(60))
  .slidingWindowSize(10)
  .minimumNumberOfCalls(10)
  .permittedNumberOfCallsInHalfOpenState(3)
  .build()
```

```
RetryConfig.custom()
  .initialInterval(Duration.ofSeconds(1))
  .maxInterval(Duration.ofSeconds(30))
  .multiplier(2.0)
  .maxAttempts(3)
  .jitter(0.1)
  .retryOnException(e -> e instanceof IOException)
  .build()
```

### 5.4 Fallback Logic

| Scenario | Behavior |
|----------|----------|
| MinIO unreachable (production) | Return HTTP 503; do not proceed with upload |
| MinIO unreachable (`storage.type=local`) | Fall back to `LocalStorageAdapter` (constitution §2.8, architecture §9.2) |
| Multipart upload failure mid-stream | Abort the multipart upload; mark video as FAILED; emit error event |
| Multipart upload chunk timeout | Retry the failed chunk up to 3 times; abort upload if all chunks fail |

### 5.5 Local Storage Fallback (Development Only)

`LocalStorageAdapter` implements `ObjectStoragePort`.

| Property | Value |
|----------|-------|
| Activation condition | `storage.type=local` explicitly set in configuration |
| Base path | `./storage/tenants/{tenantId}/videos/{videoId}/` |
| Production use | Forbidden (constitution §2.8); only `minio` is permitted for production |

---

## 6. Streaming Guarantee Enforcement

### 6.1 Prohibited Patterns

The following patterns are **forbidden** across all integrations:

| Pattern | Reason |
|---------|--------|
| `byte[] fileContent = inputStream.readAllBytes()` | Violates constitution §2.3 — entire file loaded into memory |
| `Files.readAllBytes(path)` for video files | Same; full file materialized in heap |
| `byte[100 * 1024 * 1024]` pre-allocated buffer | Wastes memory; defeats streaming |
| Caching full video in Redis/memory for processing | Memory pressure; no persistence benefit |

### 6.2 Required Patterns

| Pattern | Usage |
|---------|-------|
| `InputStream` from servlet request | Passed directly to MinIO upload, never materialized fully |
| `TransferManager` ( MinIO SDK) with multipart upload | Handles chunked uploads with streaming chunks |
| `byte[]` buffer ≤ chunk size (5MB) | Allocated per-chunk, discarded after upload |
| `ReadableByteChannel` | Used for YouTube download to YouTube upload streaming |

### 6.3 Memory Budget

| Component | Max Memory |
|-----------|-----------|
| Upload buffer per request | 5MB (chunk size) |
| Concurrent uploads per tenant | 10 (constitution §5.4) |
| Max memory for uploads per tenant | 50MB (10 × 5MB) |
| No other component may hold video data | — |

---

## 7. Metrics (Micrometer)

| Metric | Type | Labels | Purpose |
|--------|------|--------|---------|
| `upload.duration` | Timer | `tenantId`, `status` | End-to-end upload processing time |
| `gemini.latency` | Timer | `tenantId` | Gemini API call duration |
| `youtube.quota.used` | Gauge | `tenantId` | Remaining YouTube quota |
| `kafka.publish.latency` | Timer | `topic` | Kafka publish duration |
| `storage.operation.errors` | Counter | `adapter`, `operation` | Count of storage failures by adapter |
| `youtube.upload.duration` | Timer | `tenantId`, `status` | YouTube upload time |
| `circuit_breaker.state` | Gauge | `integration`, `state` | Circuit breaker state (0=CLOSED, 1=OPEN, 2=HALF_OPEN) |

All metrics are exposed via `/actuator/prometheus` per constitution §9.

---

*Version: 1.0.0*
*Status: Draft*
*Last Updated: 2026-07-28*