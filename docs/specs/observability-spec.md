# InfluencerAPP — Observability Specification

## 1. Overview

This document defines the logging format, correlation ID propagation, and metrics instrumentation for the InfluencerAPP platform. It is derived from the [Project Constitution](../constitution.md), the [Architecture Specification](../architecture.md), the [Data Model Specification](arq/data-model.md), and the [Integrations Specification](integrations-spec.md).

---

## 2. Structured Logging

### 2.1 Format

All logs are emitted in JSON format using the Logstash encoder (constitution §3.2). JSON logging enables structured ingestion by log aggregation systems (ELK, Datadog, Grafana Loki).

### 2.2 Required Fields

Every log entry MUST include the following fields:

| Field | Type | Description |
|-------|------|-------------|
| `timestamp` | `ISO-8601 UTC` | When the log event occurred |
| `level` | `String` | Log level: `TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR` |
| `service` | `String` | Service name: `influencerapp` |
| `tenantId` | `String` | Tenant identifier from `SecurityContext`; present on all tenant-scoped operations |
| `correlationId` | `UUID` | Unique request identifier for end-to-end traceability |
| `message` | `String` | Human-readable log message |
| `error` | `object` | Present only on `ERROR` level; contains `exception`, `message`, `stackTrace` |

### 2.3 Log Level Usage

| Level | Usage |
|-------|-------|
| `ERROR` | Failures requiring attention: external call failures, circuit breaker opens, decryption errors, DLQ routing, unhandled exceptions |
| `WARN` | Non-fatal issues: Gemini placeholder fallback triggered, token refresh triggered, retry attempts, DLQ events |
| `INFO` | Business events: video received, video published, channel registered, HTTP request completed with status code |
| `DEBUG` | Diagnostic details: HTTP request/response headers, Kafka consumer offsets, circuit breaker state transitions |
| `TRACE` | Very low-level: individual HTTP calls to external services, chunk-level MinIO upload progress |

### 2.4 JSON Log Example

```json
{
  "timestamp": "2026-07-28T18:00:00Z",
  "level": "INFO",
  "service": "influencerapp",
  "tenantId": "tenant-123",
  "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "message": "Video received and stored",
  "videoId": "vid-456",
  "storagePath": "tenants/tenant-123/videos/vid-456/intro.mp4"
}
```

### 2.5 Error Log Example

```json
{
  "timestamp": "2026-07-28T18:01:30Z",
  "level": "ERROR",
  "service": "influencerapp",
  "tenantId": "tenant-123",
  "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "message": "YouTube upload failed",
  "videoId": "vid-456",
  "error": {
    "exception": "YouTubeUploadException",
    "message": "Quota exhausted for channel ch-789",
    "stackTrace": "..."
  }
}
```

### 2.6 Implementation Requirements

- Use `@Slf4j` (Lombok) for all classes requiring logging (constitution §17, §3.4).
- No `System.out.println` or `System.err.println` (constitution §3.4).
- Never log `access_token`, `refresh_token`, or decrypted token values (security-spec §4.4).
- Never log encryption keys or `JWT_SECRET`.
- Tenant ID and correlation ID are included in every log entry at `INFO` level and above.

---

## 3. Correlation ID Propagation

### 3.1 Purpose

The `correlationId` uniquely identifies a single user request across all system boundaries: HTTP ingress → application layer → outbound adapters → Kafka events → downstream consumers (constitution §9.3).

### 3.2 HTTP Ingress

1. **Entry point**: `JwtAuthFilter` or the controller layer generates a `correlationId` if none is present in the request header `X-Correlation-Id`.
2. **Storage**: The `correlationId` is stored in a `ThreadLocal` context (`CorrelationContext`) for the duration of the request.
3. **Propagation**: All log entries, Kafka event payloads, and outbound HTTP calls include the `correlationId`.
4. **Response header**: The `correlationId` is returned to the client in the response header `X-Correlation-Id`.

### 3.3 Internal Thread Propagation

When the `UploadVideoUseCase` spawns asynchronous operations (e.g., `@Async` for Gemini call, Kafka publish), the `correlationId` must be propagated to the new thread:

| Mechanism | Usage |
|-----------|-------|
| `CorrelationContext` | `ThreadLocal` holder; copied to new threads before dispatch |
| `TaskDecorator` (Spring `ThreadPoolTaskExecutor`) | Wraps `Runnable`/`Callable` to copy `correlationId` from parent thread |
| Kafka producer headers | `correlationId` added as a Kafka record header on `video-received` and `video-published` topics |

### 3.4 Kafka Propagation

When an `OutboxPublisher` publishes an event to Kafka, the `correlationId` from the `outbox_events` row is included in the Kafka record headers. When a consumer processes the event, the `correlationId` is extracted from headers and set in the `CorrelationContext` before business logic executes.

### 3.5 Kafka Event Payload Correlation

Every Kafka event includes `correlationId` at the top level of the payload (kafka-events.md §4.1):

```json
{
  "schemaVersion": "1.0.0",
  "tenantId": "tenant-123",
  "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "videoId": "vid-456",
  "channelId": "ch-789",
  "timestamp": "2026-07-28T17:00:00Z",
  "payload": { ... }
}
```

### 3.6 Correlation ID Flow Diagram

```
HTTP Client
   │
   │ POST /api/videos/upload
   │ X-Correlation-Id: <existing or generated>
   ▼
JwtAuthFilter
   │ └─ Extract or generate correlationId → store in CorrelationContext (ThreadLocal)
   │ └─ Log: "Request received" (correlationId present)
   ▼
UploadVideoUseCase
   │ ├─ Log: "Storing video to MinIO" (correlationId propagated)
   │ └─ OutboxPublisher → saves outbox_events row with correlationId
   ▼
OutboxPublisher
   │ ├─ Poll outbox_events
   │ ├─ Publish to Kafka with correlationId in record header
   │ └─ Log: "Event published to video-received" (correlationId present)
   ▼
Kafka (video-received topic)
   │
   ▼
VideoReceivedConsumer
   │ ├─ Extract correlationId from Kafka record header
   │ ├─ Set in CorrelationContext
   │ ├─ Log: "Processing video-received event" (correlationId present)
   │ └─ Execute processing pipeline
```

### 3.7 Implementation

| Component | Location | Responsibility |
|-----------|----------|----------------|
| `CorrelationContext` | `domain` (utility class) | `ThreadLocal<String>` holder for `correlationId` |
| `CorrelationFilter` | `infrastructure.adapter.http` | Extracts `X-Correlation-Id` from HTTP request header; generates UUID if absent; stores in `CorrelationContext` |
| `CorrelationTaskDecorator` | `infrastructure.config` | Wraps `Runnable`/`Callable` to copy `correlationId` from parent thread to worker thread |
| `KafkaHeaderEnricher` | `infrastructure.adapter.kafka` | Adds `correlationId` from `CorrelationContext` to Kafka record headers on publish |
| `KafkaHeaderExtractor` | `infrastructure.adapter.kafka` | Extracts `correlationId` from Kafka record headers and sets in `CorrelationContext` on consume |

---

## 4. Micrometer Metrics

### 4.1 Registry

Metrics are registered via Micrometer with a `MeterRegistry` (Prometheus-compatible). All metrics are exposed at `/actuator/prometheus`.

### 4.2 Metric: `upload.duration`

| Property | Value |
|----------|-------|
| Type | Timer |
| Labels | `tenantId`, `status` (`RECEIVED`, `PROCESSING`, `UPLOADING`, `PUBLISHED`, `FAILED`) |
| Purpose | Measure end-to-end video upload processing time; track per-tenant and per-status p50/p95/p99 |
| Emitted at | Each video status transition |

### 4.3 Metric: `gemini.latency`

| Property | Value |
|----------|-------|
| Type | Timer |
| Labels | `tenantId` |
| Purpose | Measure Gemini API call duration for monitoring free-tier rate limits |
| Emitted at | Each call to `AITextGenerationPort.generateTitleAndDescription()` |

### 4.4 Metric: `youtube.quota.used`

| Property | Value |
|----------|-------|
| Type | Gauge |
| Labels | `tenantId` |
| Purpose | Track remaining YouTube API quota per tenant; alerts when quota drops below threshold |
| Emitted at | Each YouTube API call (upload + token refresh) |

### 4.5 Metric: `kafka.publish.latency`

| Property | Value |
|----------|-------|
| Type | Timer |
| Labels | `topic` |
| Purpose | Measure time to publish an event to Kafka topic; detect broker latency |
| Emitted at | Each `OutboxPublisher` → Kafka publish operation |

### 4.6 Metric: `storage.operation.errors`

| Property | Value |
|----------|-------|
| Type | Counter |
| Labels | `adapter` (`MinIOStorageAdapter`, `LocalStorageAdapter`), `operation` (`upload`, `download`, `delete`) |
| Purpose | Count storage failures by adapter and operation type |
| Emitted at | Each storage operation failure |

### 4.7 Metric: `youtube.upload.duration`

| Property | Value |
|----------|-------|
| Type | Timer |
| Labels | `tenantId`, `status` (`SUCCESS`, `FAILED`) |
| Purpose | Measure YouTube upload duration; detect slow uploads or failures |
| Emitted at | Completion of YouTube upload (success or failure) |

### 4.8 Metric: `circuit_breaker.state`

| Property | Value |
|----------|-------|
| Type | Gauge |
| Labels | `integration` (`gemini`, `youtube`), `state` (`0=CLOSED`, `1=OPEN`, `2=HALF_OPEN`) |
| Purpose | Monitor circuit breaker state per integration; alert when state transitions to OPEN |
| Emitted at | On each state transition (CLOSED → OPEN, OPEN → HALF_OPEN, HALF_OPEN → CLOSED) |

### 4.9 Metric: `security.decryption.failures`

| Property | Value |
|----------|-------|
| Type | Counter |
| Labels | `operation` (`decrypt`) |
| Purpose | Count token decryption failures; alert on non-zero values |
| Emitted at | Each decryption failure in `TokenEncryptionService` |

### 4.10 Metric: `kafka.consumer.lag`

| Property | Value |
|----------|-------|
| Type | Gauge |
| Labels | `topic`, `consumerGroup` |
| Purpose | Track Kafka consumer lag per topic and consumer group; alert on sustained lag > 5 seconds |
| Emitted at | Polling interval (every 30 seconds) |

### 4.11 Metric: `kafka.outbox.pending`

| Property | Value |
|----------|-------|
| Type | Gauge |
| Labels | None |
| Purpose | Number of PENDING rows in `outbox_events`; alert if growing unbounded |
| Emitted at | Each `OutboxPublisher` poll cycle |

### 4.12 Metric: `kafka.dlq.size`

| Property | Value |
|----------|-------|
| Type | Gauge |
| Labels | `topic` (`video-received-dlq`, `video-published-dlq`) |
| Purpose | Number of events in each DLQ; alert on non-zero for investigation |
| Emitted at | Each monitoring check |

### 4.13 Metric: `http.request.duration`

| Property | Value |
|----------|-------|
| Type | Timer |
| Labels | `method` (`GET`, `POST`), `path` (endpoint path), `status` (HTTP status code) |
| Purpose | Measure HTTP request latency across all endpoints |
| Emitted at | Each HTTP request completion via Micrometer's `ServerMeterRegistry` or Spring Boot Actuator |

### 4.14 Metrics Summary Table

| Metric | Type | Key Labels | Purpose |
|--------|------|-----------|---------|
| `upload.duration` | Timer | `tenantId`, `status` | End-to-end upload latency |
| `gemini.latency` | Timer | `tenantId` | Gemini API latency |
| `youtube.quota.used` | Gauge | `tenantId` | Remaining YouTube quota |
| `kafka.publish.latency` | Timer | `topic` | Kafka publish latency |
| `storage.operation.errors` | Counter | `adapter`, `operation` | Storage failures |
| `youtube.upload.duration` | Timer | `tenantId`, `status` | YouTube upload latency |
| `circuit_breaker.state` | Gauge | `integration`, `state` | Circuit breaker state |
| `security.decryption.failures` | Counter | `operation` | Decryption failures |
| `kafka.consumer.lag` | Gauge | `topic`, `consumerGroup` | Consumer lag |
| `kafka.outbox.pending` | Gauge | — | Pending outbox events |
| `kafka.dlq.size` | Gauge | `topic` | DLQ event count |
| `http.request.duration` | Timer | `method`, `path`, `status` | HTTP request latency |

### 4.15 Implementation

All metrics are registered via Micrometer's `MeterRegistry` and automatically exposed via Spring Boot Actuator at `/actuator/prometheus` for Prometheus scraping (constitution §9).

```java
@Component
@Slf4j
public class MetricsEmitter {

    private final MeterRegistry registry;

    public MetricsEmitter(MeterRegistry registry) {
        this.registry = registry;
    }

    // Usage examples
    public void recordUploadDuration(Duration duration, String tenantId, VideoStatus status) {
        registry.timer("upload.duration", "tenantId", tenantId, "status", status.name())
            .record(duration);
    }

    public void recordGeminiLatency(Duration duration, String tenantId) {
        registry.timer("gemini.latency", "tenantId", tenantId)
            .record(duration);
    }

    public void incrementStorageErrors(String adapter, String operation) {
        registry.counter("storage.operation.errors", "adapter", adapter, "operation", operation)
            .increment();
    }
}
```

---

## 5. Health Check

Per constitution §9.3, `GET /api/health` reports the status of all external dependencies:

```json
{
  "status": "UP",
  "components": {
    "postgresql": { "status": "UP" },
    "kafka": { "status": "UP" },
    "minio": { "status": "UP" },
    "youtubeApi": { "status": "UP" }
  }
}
```

| Component | Check Method |
|-----------|-------------|
| PostgreSQL | `SELECT 1` query |
| Kafka | `AdminClient.describeCluster()` |
| MinIO | `MinIOClient.statBucket()` |
| YouTube API | `YouTube.credentials().youtube().v3().channelList(...).execute()` (lightweight list call) |

If any component is DOWN, overall `status` is `DOWN`.

---

## 6. Observability Summary

| Concern | Implementation |
|---------|---------------|
| Log format | JSON (Logstash encoder) with `timestamp`, `level`, `service`, `tenantId`, `correlationId`, `message`, `error` |
| Correlation ID | Generated at HTTP ingress (`X-Correlation-Id` header or UUID); propagated via `ThreadLocal`, Kafka record headers, and async thread decorators |
| Metrics | Micrometer with 13 defined metrics; exposed at `/actuator/prometheus` |
| Health | `/api/health` endpoint reporting PostgreSQL, Kafka, MinIO, and YouTube API connectivity |
| Correlation ID in logs | Present on every log entry at INFO and above |
| Correlation ID in Kafka | Present in event payload `correlationId` field and in Kafka record headers |

---

*Version: 1.0.0*
*Status: Draft*
*Last Updated: 2026-07-28*