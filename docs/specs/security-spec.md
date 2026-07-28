# InfluencerAPP — Security Specification

## 1. Overview

This document defines the security architecture for the InfluencerAPP platform, covering JWT validation, tenant extraction, and encryption of OAuth2 tokens at rest. It is derived from the [Project Constitution](../constitution.md), the [Architecture Specification](../architecture.md), and the [Data Model Specification](data-model.md).

---

## 2. JWT Validation Filter

### 2.1 Filter Name

`JwtAuthFilter` — implemented as a Spring `OncePerRequestFilter` in the `infrastructure.adapter.http` package.

### 2.2 Scope

All endpoints require valid JWT Bearer tokens except:

| Endpoint | Method | Auth Required |
|----------|--------|---------------|
| `/api/auth/register` | POST | No |
| `/api/auth/login` | POST | No |
| `/api/health` | GET | No |

All other endpoints require a `Authorization: Bearer <token>` header (constitution §4.1, architecture §12.1).

### 2.3 Validation Steps

1. **Extract** the token from the `Authorization` header. If the header is missing or does not start with `Bearer `, return `401` with RFC 7807 problem detail.
2. **Parse** the JWT using the signing key from `${JWT_SECRET}`. Supported algorithms: HS256 or RS256 (architecture §12.1).
3. **Validate** the token:
   - Signature must be valid.
   - `exp` claim must be in the future.
   - `sub` claim (userId) must be present.
   - `tenantId` claim must be present.
4. **Reject** tokens that fail any validation step with `401 Unauthorized`.

### 2.4 Token Claim Mapping

| Claim | Type | Description |
|-------|------|-------------|
| `sub` | `String` (UUID) | User identifier — used as `TenantId` at the port boundary |
| `tenantId` | `String` (UUID) | Tenant identifier for multi-tenancy isolation |
| `exp` | `NumericDate` | Token expiration time |
| `iat` | `NumericDate` | Token issuance time |

### 2.5 Error Response

Failed validation returns RFC 7807 `application/problem+json`:

```json
{
  "type": "https://influencerapp/errors/unauthorized",
  "title": "Unauthorized",
  "status": 401,
  "detail": "JWT token is missing, expired, or invalid",
  "instance": "/api/videos"
}
```

### 2.6 Implementation Location

`infrastructure.adapter.http.JwtAuthFilter` — registered as a `FilterRegistrationBean` or via Spring Security `HttpSecurity` configuration in `infrastructure.config.SecurityConfig`.

---

## 3. Tenant ID Extraction

### 3.1 Extraction Point

The `tenantId` is extracted from the validated JWT at the HTTP adapter layer (architecture §6.1). The filter sets the `TenantId` on the `SecurityContext` so that all downstream code has access to it without re-parsing the token.

### 3.2 Propagation

The `TenantId` flows through the entire request lifecycle:

1. **HTTP Adapter** (`JwtAuthFilter`): Extract `tenantId` from JWT principal → set on `SecurityContext`.
2. **Use Case Layer**: Each use case receives `TenantId` as a method parameter. The use case retrieves it from the authenticated principal. It validates that the resource belongs to the tenant before any operation (architecture §12.2).
3. **Repository Layer**: All repository queries include `WHERE tenant_id = :tenantId` (architecture §6.2). The `tenantId` is passed as a query parameter via `@TenantId`.
4. **Storage Layer**: Object storage keys are prefixed with `tenants/{tenantId}/` (architecture §6.3). The `TenantId` is injected into every storage path.
5. **Kafka Producer**: Every event payload includes `tenantId` at the top level (architecture §6.4, constitution §2).

### 3.3 Cross-Tenant Isolation Enforcement

- A user MUST NOT access resources belonging to a different tenant.
- If a `TenantId` mismatch is detected between the JWT and the target resource, throw `TenantIsolationViolationException` (constitution §3.4).
- This check applies to every HTTP endpoint, repository query, storage operation, and Kafka publish.
- Cross-tenant data leakage is a P0 violation (constitution §2).

### 3.4 Implementation Rules

| Layer | Rule |
|-------|------|
| Domain | `TenantId` is a `@Embeddable` value object, immutable, used in all aggregate roots |
| Application | Use cases validate tenant ownership before every write and on every read |
| Infrastructure | Repositories, JPA entity graph filters, and storage adapters enforce `tenantId` prefix |

---

## 4. Token Encryption (JCE — AES/GCM)

### 4.1 Purpose

OAuth2 tokens (`access_token`, `refresh_token`, `token_expiry`) stored in the `channels` table MUST be encrypted at rest (constitution §4.3, architecture §12.3).

### 4.2 Algorithm

| Property | Value |
|----------|-------|
| Algorithm | AES/GCM/NoPadding |
| Key Size | 256 bits |
| IV Size | 96 bits (12 bytes) |
| Tag Length | 128 bits |
| Mode | GCM (Galois/Counter Mode) |

GCM provides both confidentiality and integrity (authenticated encryption). The `NoPadding` specifier is used because GCM operates on block boundaries natively and does not require padding.

### 4.3 Key Management

| Source | Priority |
|--------|----------|
| `${ENCRYPTION_KEY}` environment variable | Primary (preferred) |
| `${JWT_SECRET}` environment variable | Fallback (if `ENCRYPTION_KEY` is not set) |

Both keys are 256-bit (32 bytes) Base64-encoded strings. The key must never appear in source code, logs, or version control (constitution §2.5).

### 4.4 Encryption Workflow

```
1. Retrieve key bytes from ${ENCRYPTION_KEY} (or fallback to ${JWT_SECRET})
2. Decode Base64 key to byte[]
3. Generate random 12-byte IV using SecureRandom
4. Initialize Cipher with AES/GCM/NoPadding
5. Initialize GCMParameterSpec with IV and 128-bit authentication tag length
6. Encrypt plaintext token bytes via Cipher.doFinal()
7. Concatenate IV + ciphertext + authentication tag (GCM appends tag automatically)
8. Store the concatenated byte array as a bytea column in PostgreSQL
```

### 4.5 Decryption Workflow

```
1. Retrieve key bytes from ${ENCRYPTION_KEY} (or fallback to ${JWT_SECRET})
2. Decode Base64 key to byte[]
3. Read bytea column from database
4. Extract first 12 bytes as IV
5. Extract remaining bytes as ciphertext+tag
6. Initialize Cipher with AES/GCM/NoPadding
7. Initialize GCMParameterSpec with extracted IV and 128-bit tag length
8. Decrypt via Cipher.doFinal()
9. Return plaintext token string
10. Wipe plaintext from memory as soon as possible
```

### 4.6 Implementation

| Component | Location | Responsibility |
|-----------|----------|----------------|
| `TokenEncryptionService` | `infrastructure.adapter.security` | Encrypt/decrypt tokens using AES/GCM. Implements encryption/decryption methods. |
| `ChannelEntity` | `infrastructure.entity` | Stores `access_token_enc`, `refresh_token_enc`, `token_expiry` as `bytea` columns |
| `YouTubeUploadAdapter` | `infrastructure.adapter.youtube` | Decrypts tokens in-memory before making YouTube API calls |

### 4.7 Column Types (Database)

| Column | PostgreSQL Type | Description |
|--------|----------------|-------------|
| `access_token_enc` | `bytea` | AES/GCM encrypted access token |
| `refresh_token_enc` | `bytea` | AES/GCM encrypted refresh token |
| `token_expiry` | `timestamptz` | Token expiry timestamp (stored in plaintext — it is not a secret, only a timestamp) |

### 4.8 Security Constraints

- Plaintext tokens MUST NEVER be written to disk, logs, or the console (constitution §2.5).
- Decrypted tokens exist in memory only for the duration of the YouTube API call.
- `@Slf4j` logging MUST NOT log token values (encrypted or decrypted).
- The `ENCRYPTION_KEY` and `JWT_SECRET` environment variables MUST be set in production (constitution §2.5).
- Rotation of encryption keys requires re-encrypting all stored tokens or migrating to the new key. Key rotation is out of scope for v1.0.

### 4.9 Error Handling

- Decryption failures (e.g., wrong key, corrupted ciphertext) MUST NOT expose stack traces or key material.
- A `DomainException` with a generic message is thrown on decryption failure.
- The event is logged at `ERROR` level (without token data) and the metric `security.decryption.failures` is incremented (constitution §9).

---

## 5. Transport Security

### 5.1 HTTPS (Production)

- All external communications MUST use TLS in production (constitution §4.6).
- TLS termination occurs at the load balancer or reverse proxy (not in the application).
- The application itself runs on HTTP internally; the reverse proxy enforces HTTPS.

### 5.2 HTTP (Development Only)

- HTTP is permitted for `localhost` development connections.
- The application MUST reject HTTP connections in production profiles.

---

## 6. Input Validation

### 6.1 Multipart Upload Validation

| Check | Constraint | Location |
|-------|-----------|----------|
| Content-Type | Must match `video/*` | `JwtAuthFilter` / controller layer |
| Content-Length | Must not exceed `spring.servlet.multipart.max-file-size` (default 100MB) | Servlet container |
| Filename | Must not contain path traversal characters (`..`, `/`, `\`) | Controller validation |
| Field presence | `file` and `channelId` must be present | Controller validation |

### 6.2 Bean Validation

All DTOs use `jakarta.validation` annotations (architecture §12.3). Validation failures return RFC 7807 `422 Unprocessable Entity` responses.

---

## 7. Security Summary

| Concern | Mechanism | Reference |
|---------|-----------|-----------|
| Authentication | JWT Bearer tokens via `JwtAuthFilter` | §2 |
| Tenant Isolation | `tenantId` extracted from JWT, enforced at every layer | §3 |
| Token Encryption at Rest | AES/GCM/NoPadding via JCE | §4 |
| Key Management | Environment variables, no hardcoded keys | §4.3 |
| Transport Security | HTTPS in production, HTTP for localhost dev | §5 |
| Input Validation | Multipart checks + Bean Validation | §6 |
| Error Handling | Generic messages, no leakage of secrets or keys | §4.9 |

---

*Version: 1.0.0*
*Status: Draft*
*Last Updated: 2026-07-28*