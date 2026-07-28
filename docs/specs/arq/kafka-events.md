# InfluencerAPP — Kafka Events Specification

## 1. Overview

This document defines the Kafka event schemas, topics, and the Outbox persistence pattern for the InfluencerAPP video publishing pipeline. It is derived from the [Project Constitution](../constitution.md), the [Architecture Specification](../architecture.md), and the [Data Model Specification](data-model.md).

All events are versioned and carried over Kafka with tenant isolation, correlation tracing, and eventual consistency guarantees.

---

## 2. Topic Naming Convention

| Rule | Detail |
|------|--------|
| **Case** | kebab-case |
| **Form** | Plural nouns |
| **Prefix** | No prefix for system topics; tenant-prefixed topics recommended for multi-tenant isolation (see §5) |
| **Schema** | Versioned via `schemaVersion` field in every payload |

Convention is enforced per architecture §7.1 and constitution §6.1.

---

## 3. Topics

| Topic | Purpose | Key |
|-------|---------|-----|
| `video-received` | Emitted after a video file is successfully uploaded to object storage and validated | `videoId` |
| `video-published` | Emitted after the video has been successfully published to YouTube | `videoId` |
| `video-received-dlq` | Dead Letter Queue for `video-received` events that fail after maximum retries | `videoId` |
| `video-published-dlq` | Dead Letter Queue for `video-published` events that fail after maximum retries | `videoId` |
| `{tenantId}-video-received` | Recommended tenant-scoped variant of `video-received` for isolated consumption | `videoId` |
| `{tenantId}-video-published` | Recommended tenant-scoped variant of `video-published` for isolated consumption | `videoId` |

Topics `video-received-dlq` and `video-published-dlq` are used when the main topic consumer cannot process an event after the retry limit (constitution §6.3, architecture §8.4).

---

## 4. Event Schemas

### 4.1 Base Event Envelope

Every event shares the following top-level envelope. All fields are required unless marked nullable.

| Field | Type | Description |
|-------|------|-------------|
| `schemaVersion` | `String` | Semantic version of the event schema; current value is `"1.0.0"` |
| `tenantId` | `String` | Tenant identifier for multi-tenancy isolation (constitution §2) |
| `correlationId` | `UUID` | Unique request correlation ID; flows from HTTP ingress through Kafka to downstream consumers (constitution §9) |
| `videoId` | `UUID` | References the `Video.videoId` aggregate |
| `channelId` | `UUID` | References the `Channel.channelId` aggregate |
| `timestamp` | `String` (ISO-8601 UTC) | Time the event was emitted |
| `payload` | `object` | Domain-specific event body |

### 4.2 Event: `video-received`

**Topic:** `video-received`

**Triggered by:** Successful upload of the video file to object storage and persistence of the `Video` aggregate in `RECEIVED` status.

**Payload shape:**

```json
{
  "schemaVersion": "1.0.0",
  "tenantId": "tenant-123",
  "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "videoId": "vid-456",
  "channelId": "ch-789",
  "timestamp": "2026-07-28T17:00:00Z",
  "payload": {
    "storagePath": "tenants/tenant-123/videos/vid-456/intro.mp4",
    "fileName": "intro.mp4",
    "fileSize": 52428800,
    "mimeType": "video/mp4",
    "checksum": "sha256:3a4f8c..."
  }
}
```

**Payload fields:**

| Field | Type | Description |
|-------|------|-------------|
| `storagePath` | `String` | Full MinIO object key; always tenant-prefixed |
| `fileName` | `String` | Original filename as uploaded |
| `fileSize` | `long` | File size in bytes |
| `mimeType` | `String` | MIME type; must match `video/*` |
| `checksum` | `String` | SHA-256 hash for integrity verification |

### 4.3 Event: `video-published`

**Topic:** `video-published`

**Triggered by:** Successful completion of the YouTube upload and the `Video` aggregate transitioning to `PUBLISHED` status.

**Payload shape:**

```json
{
  "schemaVersion": "1.0.0",
  "tenantId": "tenant-123",
  "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "videoId": "vid-456",
  "channelId": "ch-789",
  "timestamp": "2026-07-28T17:01:30Z",
  "payload": {
    "storagePath": "tenants/tenant-123/videos/vid-456/intro.mp4",
    "fileName": "intro.mp4",
    "fileSize": 52428800,
    "mimeType": "video/mp4",
    "checksum": "sha256:3a4f8c...",
    "youtubeVideoId": "dQw4w9WgXcQ",
    "youtubeUrl": "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
  }
}
```

**Payload fields (additional to `video-received`):**

| Field | Type | Description |
|-------|------|-------------|
| `youtubeVideoId` | `String` | YouTube-assigned video identifier |
| `youtubeUrl` | `String` (URI) | Resolvable YouTube watch URL |

---

## 5. Key Constraints

### 5.1 No Binary Data

Event payloads MUST NOT contain binary data (constitution §6.4). Video file references use storage paths/URIs only. The `storagePath` field points to the object in MinIO from which consumers can stream the file if needed.

### 5.2 Tenant Isolation

Every event payload includes `tenantId` at the top level (constitution §2). Consumers MUST filter by tenant context:

- **Shared topics** (`video-received`, `video-published`): consumers include `tenantId` in the event and filter accordingly.
- **Tenant-scoped topics** (`{tenantId}-video-received`, `{tenantId}-video-published`): recommended for multi-tenant deployments to guarantee isolation at the broker level.

### 5.3 Schema Versioning

All event payloads include `schemaVersion` (constitution §6.1). Consumers MUST handle unknown `schemaVersion` values by routing the event to the DLQ rather than crashing. Schema evolution follows semantic versioning.

### 5.4 Consumer Idempotency

Consumers MUST be idempotent (constitution §6.2). The `processed_events` table (see `docs/specs/arq/data-model.md` §3.3) tracks consumed `eventId` values with a unique constraint. Duplicate event delivery MUST NOT trigger duplicate YouTube uploads or state changes.

---

## 6. Dead Letter Queue (DLQ)

| Topic | Routing Condition |
|-------|-------------------|
| `video-received-dlq` | Consumer fails after 3 retry attempts with exponential backoff |
| `video-published-dlq` | Consumer fails after 3 retry attempts with exponential backoff |

Failure rate threshold: 50% of events over a 10-second window triggers circuit breaker behavior on the consumer side (constitution §7.2). DLQ events are available for manual inspection and reprocessing.

---

## 7. Outbox Pattern (Persistent Event Buffer)

### 7.1 Rationale

When Kafka is unavailable, the system MUST not lose events. The Outbox pattern ensures that domain state changes and event emissions are atomic: both succeed or both fail within the same database transaction (architecture §8.5).

### 7.2 Table: `outbox_events`

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | `bigserial` | NOT NULL | auto | Primary key |
| `aggregateId` | `varchar(255)` | NOT NULL | — | ID of the aggregate that produced the event (e.g., `videoId`) |
| `tenantId` | `varchar(255)` | NOT NULL | — | Tenant isolation key |
| `topic` | `varchar(255)` | NOT NULL | — | Target Kafka topic name |
| `eventType` | `varchar(255)` | NOT NULL | — | Type discriminator (`VideoReceived`, `VideoPublished`) |
| `payload` | `text` | NOT NULL | — | JSON-serialized event body matching the schema in §4 |
| `schemaVersion` | `varchar(20)` | NOT NULL | `"1.0.0"` | Schema version for forward compatibility |
| `status` | `varchar(20)` | NOT NULL | `PENDING` | `PENDING`, `PUBLISHED`, `FAILED` |
| `retryCount` | `int` | NOT NULL | `0` | Number of publish attempts |
| `createdAt` | `timestamptz` | NOT NULL | `NOW()` | When the outbox entry was created |
| `publishedAt` | `timestamptz` | Nullable | — | When the event was successfully published to Kafka |
| `errorMessage` | `text` | Nullable | — | Last error message if publish failed |

**Unique constraint:** `(aggregateId, eventType)` — prevents duplicate outbox entries for the same aggregate transition.

### 7.3 OutboxPublisher

The `OutboxPublisher` is a scheduled or streaming process that:

1. Polls `outbox_events` for rows with `status = PENDING`.
2. Publishes each event to the corresponding Kafka topic.
3. On success: sets `status = PUBLISHED` and records `publishedAt`.
4. On failure: increments `retryCount`, sets `status = FAILED`, and records `errorMessage`.
5. Events with `retryCount >= 3` and `status = FAILED` are considered permanently failed outbox publish attempts. These rows remain in the `outbox_events` table for manual inspection and are flagged via the `kafka.outbox.pending` and `kafka.outbox.failed` metrics. They are NOT routed to the consumer DLQ topics (`video-received-dlq`, `video-published-dlq`), which are reserved exclusively for consumer-side processing failures after retry exhaustion.

### 7.4 Transactional Guarantees

The Outbox pattern guarantees exactly-once event emission in the following way:

1. The use case saves the aggregate state (e.g., `Video` transition to `RECEIVED`) and the `outbox_events` row in a **single database transaction**.
2. `OutboxPublisher` reads committed rows and publishes to Kafka outside the transaction.
3. The publisher uses Kafka producer idempotency (`enable.idempotence=true`) to prevent duplicate publishes on retries.

---

## 8. Event Flow

```
HTTP POST /api/videos/upload
       │
       ▼
  ┌─────────────────────┐
  │  UploadVideoUseCase  │
  │  1. Validate input   │
  │  2. Store to MinIO   │
  │  3. Persist Video    │
  │  4. Write outbox     │───► outbox_events (PENDING)
  │     event (tx)       │
  └──────────┬───────────┘
             │ (transaction committed)
             ▼
  ┌─────────────────────┐
  │  OutboxPublisher     │───► Kafka: video-received
  └─────────────────────┘
             │
             ▼
  ┌─────────────────────┐
  │  VideoProcessingSvc  │
  │  1. Generate metadata│
  │  2. Update Video     │
  │  3. Write outbox     │───► outbox_events (PENDING)
  │     event (tx)       │
  └──────────┬───────────┘
             │
             ▼
  ┌─────────────────────┐
  │  YouTubeUploadSvc    │
  │  1. Stream from MinIO│
  │  2. Upload to YT API │
  │  3. Update Video     │
  │     to PUBLISHED     │
  │  4. Write outbox     │───► outbox_events (PENDING)
  │     event (tx)       │
  └──────────┬───────────┘
             │
             ▼
  ┌─────────────────────┐
  │  OutboxPublisher     │───► Kafka: video-published
  └─────────────────────┘
```

---

## 9. Resilience

Failure scenarios and their handling per constitution §7 and architecture §13:

| Failure | Behavior |
|---------|----------|
| Kafka unavailable at publish time | Events persist in `outbox_events`; `OutboxPublisher` retries with exponential backoff |
| Kafka unavailable for duration | In-memory buffer for development; persistent outbox for production (constitution §7.5) |
| Consumer processing failure | Idempotent consumer checks `processed_events`; retry up to 3 times then route to DLQ |
| DLQ event | Manual inspection; no automatic retry; alerts via monitoring metrics |

---

## 10. Monitoring

| Metric | Type | Description |
|--------|------|-------------|
| `kafka.publish.latency` | Timer | Time to publish an event to Kafka (per topic) |
| `kafka.outbox.pending` | Gauge | Number of PENDING rows in `outbox_events` |
| `kafka.dlq.size` | Gauge | Number of events in each DLQ topic |
| `kafka.consumer.lag` | Gauge | Per-consumer-group lag per topic |

All metrics are emitted via Micrometer and exposed through `/actuator/prometheus` (constitution §9). Correlation ID flows from HTTP request through Kafka events to consumer logs (constitution §9).

---

*Version: 1.0.0*
*Status: Draft*
*Last Updated: 2026-07-28*