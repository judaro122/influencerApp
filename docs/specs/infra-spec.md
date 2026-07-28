# InfluencerAPP — Infrastructure Specification

## 1. Overview

This document defines the Docker compose configuration, database migration strategy (Flyway), and all required system environment variables for the InfluencerAPP platform. It is derived from the [Project Constitution](../constitution.md), the [Architecture Specification](../architecture.md), the [Data Model Specification](arq/data-model.md), and the [Integrations Specification](integrations-spec.md).

---

## 2. Docker Compose

### 2.1 File: `docker-compose.yml` (project root)

The `docker-compose.yml` defines all services required for development, testing, and production-like staging environments (constitution §10.1).

### 2.2 Services

| Service | Image | Purpose |
|---------|-------|---------|
| `app` | `influencerapp:1.0.0` | Spring Boot application (built from Dockerfile) |
| `postgres` | `postgres:15` | PostgreSQL database |
| `kafka` | `confluentinc/cp-kafka:7.5.0` | Apache Kafka |
| `zookeeper` | `confluentinc/cp-zookeeper:7.5.0` | Apache ZooKeeper (Kafka coordination) |
| `minio` | `minio/minio:latest` | S3-compatible object storage |
| `flyway` | `flyway/flyway:10.7.0` | Database migration executor |

### 2.3 docker-compose.yml

```yaml
version: "3.9"

services:
  app:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: influencerapp
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: docker
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/influencerapp
      SPRING_DATASOURCE_USERNAME: influencerapp
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
      SPRING_FLYWAY_ENABLED: "true"
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092
      SPRING_KAFKA_CONSUMER_GROUP_ID: influencerapp-group
      SPRING_MINIO_ENDPOINT: http://minio:9000
      SPRING_MINIO_ACCESS_KEY: minioadmin
      SPRING_MINIO_SECRET_KEY: minioadmin
      SPRING_MINIO_BUCKET: video-bucket
      ENCRYPTION_KEY: ${ENCRYPTION_KEY}
      JWT_SECRET: ${JWT_SECRET}
      GEMINI_API_KEY: ${GEMINI_API_KEY}
      SPRING_SERVLET_MULTIPART_MAX_FILE_SIZE: 100MB
      SPRING_SERVLET_MULTIPART_MAX_REQUEST_SIZE: 100MB
      STORAGE_TYPE: minio
      LOGGING_LEVEL_ROOT: INFO
      MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE: health,metrics,prometheus
    depends_on:
      postgres:
        condition: service_healthy
      kafka:
        condition: service_healthy
      minio:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/api/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s
    restart: unless-stopped
    networks:
      - influencerapp-network
    volumes:
      - app-logs:/var/log/influencerapp

  postgres:
    image: postgres:15
    container_name: influencerapp-postgres
    ports:
      - "5432:5432"
    environment:
      POSTGRES_DB: influencerapp
      POSTGRES_USER: influencerapp
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres-data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U influencerapp -d influencerapp"]
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped
    networks:
      - influencerapp-network

  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    container_name: influencerapp-zookeeper
    ports:
      - "2181:2181"
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    healthcheck:
      test: ["CMD", "zookeeper-shell", "localhost:2181", "ls", "/"]
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped
    networks:
      - influencerapp-network

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    container_name: influencerapp-kafka
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092,PLAINTEXT_HOST://localhost:29092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"
      KAFKA_DEFAULT_REPLICATION_FACTOR: 1
    depends_on:
      zookeeper:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "kafka-broker-api-versions.sh", "--bootstrap-server", "kafka:9092"]
      interval: 15s
      timeout: 10s
      retries: 5
    restart: unless-stopped
    networks:
      - influencerapp-network

  minio:
    image: minio/minio:latest
    container_name: influencerapp-minio
    ports:
      - "9000:9000"
      - "9001:9001"
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: ${MINIO_ROOT_PASSWORD}
    command: server /data --console-address ":9001"
    volumes:
      - minio-data:/data
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:9000/minio/health/live"]
      interval: 10s
      timeout: 5s
      retries: 3
    restart: unless-stopped
    networks:
      - influencerapp-network

  flyway:
    image: flyway/flyway:10.7.0
    container_name: influencerapp-flyway
    environment:
      FLYWAY_URL: jdbc:postgresql://postgres:5432/influencerapp
      FLYWAY_USER: influencerapp
      FLYWAY_PASSWORD: ${DB_PASSWORD}
      FLYWAY_LOCATIONS: classpath:db/migration
      FLYWAY_CLEAN_DISABLED: "true"
    volumes:
      - ./src/main/resources/db/migration:/flyway/sql
      - ./flyway/sql:/flyway/custom
    depends_on:
      postgres:
        condition: service_healthy
    networks:
      - influencerapp-network
    entrypoint: ["/flyway/flyway", "migrate"]

volumes:
  postgres-data:
    driver: local
  minio-data:
    driver: local
  app-logs:
    driver: local

networks:
  influencerapp-network:
    driver: bridge
```

### 2.4 Dockerfile

The `Dockerfile` at the project root builds the Spring Boot application:

```dockerfile
FROM eclipse-temurin:17-jre-alpine AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 2.5 Docker Compose Profiles

| Profile | Services | Purpose |
|---------|----------|---------|
| `default` (all) | `app`, `postgres`, `kafka`, `zookeeper`, `minio` | Full development environment |
| `ci` | `app`, `postgres`, `kafka`, `minio` | CI pipeline — excludes ZooKeeper (auto-created) |
| `dev` | `app`, `postgres` | Lightweight development — requires external Kafka and MinIO or `storage.type=local` |

Usage: `docker-compose --profile ci up`

---

## 3. Database Migrations (Flyway)

### 3.1 Migration Strategy

Flyway is the canonical migration tool (constitution §10.1, architecture §16.3). Liquibase is not used. All migrations are versioned SQL files executed in order on application startup.

### 3.2 Migration Location

```
src/main/resources/db/migration/
├── V1__init.sql
├── V2__add_idempotency_keys.sql
├── V3__add_processed_events.sql
└── V4__add_outbox_events.sql
```

### 3.3 Migration: V1 — Init

**File:** `src/main/resources/db/migration/V1__init.sql`

```sql
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE tenants (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_tenant_id ON users(tenant_id);
CREATE UNIQUE INDEX idx_users_tenant_email ON users(tenant_id, email);

CREATE TABLE channels (
    channel_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    youtube_channel_id VARCHAR(255) NOT NULL,
    youtube_channel_title VARCHAR(500) NOT NULL,
    access_token_enc BYTEA NOT NULL,
    refresh_token_enc BYTEA NOT NULL,
    token_expiry TIMESTAMPTZ NOT NULL,
    scope VARCHAR(255) NOT NULL DEFAULT 'https://www.googleapis.com/auth/youtube.upload',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_channels_tenant_id ON channels(tenant_id);
CREATE UNIQUE INDEX idx_channels_tenant_youtube ON channels(tenant_id, youtube_channel_id);

CREATE TABLE videos (
    video_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    channel_id UUID NOT NULL REFERENCES channels(channel_id),
    filename VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    mime_type VARCHAR(255) NOT NULL,
    checksum VARCHAR(255) NOT NULL,
    storage_path VARCHAR(1000) NOT NULL,
    title VARCHAR(500),
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'RECEIVED',
    youtube_video_id VARCHAR(255),
    error_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_videos_tenant_id ON videos(tenant_id);
CREATE INDEX idx_videos_tenant_status ON videos(tenant_id, status);
CREATE INDEX idx_videos_channel_id ON videos(channel_id);
CREATE UNIQUE INDEX idx_videos_tenant_id ON videos(tenant_id, video_id);
```

### 3.4 Migration: V2 — Idempotency Keys

**File:** `src/main/resources/db/migration/V2__add_idempotency_keys.sql`

```sql
CREATE TABLE idempotency_keys (
    idempotency_key VARCHAR(255) NOT NULL,
    tenant_id UUID NOT NULL,
    response_body TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (idempotency_key)
);

CREATE INDEX idx_idempotency_keys_tenant_id ON idempotency_keys(tenant_id);
```

### 3.5 Migration: V3 — Processed Events

**File:** `src/main/resources/db/migration/V3__add_processed_events.sql`

```sql
CREATE TABLE processed_events (
    event_id VARCHAR(255) NOT NULL,
    tenant_id UUID NOT NULL,
    topic VARCHAR(255) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (event_id)
);

CREATE INDEX idx_processed_events_tenant_id ON processed_events(tenant_id);
CREATE INDEX idx_processed_events_topic ON processed_events(topic);
```

### 3.6 Migration: V4 — Outbox Events

**File:** `src/main/resources/db/migration/V4__add_outbox_events.sql`

```sql
CREATE TABLE outbox_events (
    id BIGSERIAL NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    tenant_id UUID NOT NULL,
    topic VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    schema_version VARCHAR(20) NOT NULL DEFAULT '1.0.0',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    published_at TIMESTAMPTZ,
    error_message TEXT,
    PRIMARY KEY (id)
);

CREATE INDEX idx_outbox_events_status ON outbox_events(status);
CREATE INDEX idx_outbox_events_tenant_id ON outbox_events(tenant_id);
CREATE INDEX idx_outbox_events_aggregate_id ON outbox_events(aggregate_id, event_type);
CREATE UNIQUE INDEX uq_outbox_events_aggregate_event ON outbox_events(aggregate_id, event_type);
```

### 3.7 Migration Naming Convention

| Rule | Detail |
|------|--------|
| Prefix | `V` followed by version number |
| Separator | Double underscore `__` (SQL standard for Flyway) |
| Suffix | `.sql` |
| Description | Snake_case, imperative tense, descriptive |
| Example | `V5__add_thumbnail_url_column.sql` |

### 3.8 Flyway Configuration (application.yml)

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: false
    clean-disabled: true
    validate-on-migrate: true
    retry-attempts: 3
    retry-delay: 5s
```

---

## 4. Environment Variables

### 4.1 Naming Convention

All environment variables use `UPPER_SNAKE_CASE` (constitution §9). They are injected via Docker Compose `environment` section, system environment, or Docker secrets in production (constitution §10.3). No `.env` files are committed (constitution §10.3).

### 4.2 Application Variables

| Variable | Type | Required | Default | Description |
|----------|------|----------|---------|-------------|
| `SPRING_PROFILES_ACTIVE` | String | Yes | `docker` | Spring profile active at runtime |
| `SPRING_DATASOURCE_URL` | String | Yes | — | JDBC URL for PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` | String | Yes | — | Database username |
| `SPRING_DATASOURCE_PASSWORD` | String | Yes | — | Database password (secret) |
| `SPRING_FLYWAY_ENABLED` | Boolean | No | `true` | Enable/disable Flyway migrations |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | String | Yes | — | Kafka bootstrap servers (`host:port`) |
| `SPRING_KAFKA_CONSUMER_GROUP_ID` | String | Yes | `influencerapp-group` | Kafka consumer group |
| `SPRING_MINIO_ENDPOINT` | String | Yes | — | MinIO endpoint URL |
| `SPRING_MINIO_ACCESS_KEY` | String | Yes | — | MinIO access key |
| `SPRING_MINIO_SECRET_KEY` | String | Yes | — | MinIO secret key (secret) |
| `SPRING_MINIO_BUCKET` | String | No | `video-bucket` | Default MinIO bucket name |
| `STORAGE_TYPE` | String | No | `minio` | Storage backend: `minio` or `local` |
| `SPRING_SERVLET_MULTIPART_MAX_FILE_SIZE` | String | No | `100MB` | Maximum upload file size |
| `SPRING_SERVLET_MULTIPART_MAX_REQUEST_SIZE` | String | No | `100MB` | Maximum multipart request size |
| `LOGGING_LEVEL_ROOT` | String | No | `INFO` | Root logging level |
| `MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE` | String | No | `health,metrics,prometheus` | Actuator endpoints to expose |

### 4.3 Security Variables

| Variable | Type | Required | Default | Description |
|----------|------|----------|---------|-------------|
| `JWT_SECRET` | String (Base64, 32 bytes min) | Yes | — | Secret key for JWT signing (constitution §4.3) |
| `ENCRYPTION_KEY` | String (Base64, 32 bytes min) | Yes | — | Key for AES/GCM token encryption; falls back to `JWT_SECRET` if not set (security-spec §4.3) |
| `DB_PASSWORD` | String | Yes | — | Database password (secret) |
| `MINIO_ROOT_PASSWORD` | String | Yes | — | MinIO root password (secret) |
| `GEMINI_API_KEY` | String | Yes | — | Google Generative AI API key for Gemini |

### 4.4 Kafka Variables (Injected by Docker Compose)

| Variable | Set By | Description |
|----------|--------|-------------|
| `KAFKA_BROKER_ID` | Kafka container | `1` |
| `KAFKA_ZOOKEEPER_CONNECT` | Kafka container | `zookeeper:2181` |
| `KAFKA_ADVERTISED_LISTENERS` | Kafka container | `PLAINTEXT://kafka:9092,PLAINTEXT_HOST://localhost:29092` |
| `KAFKA_AUTO_CREATE_TOPICS_ENABLE` | Kafka container | `true` |
| `KAFKA_DEFAULT_REPLICATION_FACTOR` | Kafka container | `1` |
| `KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR` | Kafka container | `1` |

### 4.5 PostgreSQL Variables (Injected by Docker Compose)

| Variable | Set By | Description |
|----------|--------|-------------|
| `POSTGRES_DB` | Postgres container | `influencerapp` |
| `POSTGRES_USER` | Postgres container | `influencerapp` |
| `POSTGRES_PASSWORD` | Via `${DB_PASSWORD}` | Database password |

### 4.6 MinIO Variables (Injected by Docker Compose)

| Variable | Set By | Description |
|----------|--------|-------------|
| `MINIO_ROOT_USER` | MinIO container | `minioadmin` |
| `MINIO_ROOT_PASSWORD` | Via `${MINIO_ROOT_PASSWORD}` | MinIO root password |

### 4.7 Secrets Management

| Environment | Mechanism |
|-------------|-----------|
| Development | Docker Compose `environment` section |
| Production | Docker secrets, Kubernetes secrets, or external secret manager (HashiCorp Vault, AWS Secrets Manager) (constitution §10.3) |

No secrets are baked into images, committed to version control, or stored in `.env` files (constitution §2.5):

```yaml
# Production secrets example
secrets:
  db_password:
    external: true
  encryption_key:
    external: true
  jwt_secret:
    external: true
  minio_root_password:
    external: true
  gemini_api_key:
    external: true
```

---

## 5. Network Topology

All services run on a single Docker bridge network (`influencerapp-network`) enabling DNS-based service discovery:

```
+------------------+       +-------------------+
|   app (8080)     |-------|  postgres:5432    |
|  influencerapp   |       |  (application DB) |
+------------------+       +-------------------+
        |
        |-------|  kafka:9092      |
        |       |  (events)         |
        |       +-------------------+
        |
        |-------|  zookeeper:2181  |
        |       |  (Kafka coord)    |
        |       +-------------------+
        |
        |-------|  minio:9000       |
                |  (object storage) |
                +-------------------+
```

The `app` container can reach all other services via Docker Compose service names as hostnames (`postgres`, `kafka`, `zookeeper`, `minio`). External clients reach the application on port `8080`. MinIO console is accessible on port `9001` for administrative purposes.

---

## 6. Volume Mounts Summary

| Volume | Container Path | Purpose |
|--------|---------------|---------|
| `postgres-data` | `/var/lib/postgresql/data` | Persistent database storage |
| `minio-data` | `/data` | Persistent object storage |
| `app-logs` | `/var/log/influencerapp` | Application logs for host access |

---

## 7. Startup Order

Flyway runs migrations before the application processes any requests. The `depends_on` conditions use Docker Compose health checks to ensure proper ordering:

1. `zookeeper` starts and becomes healthy
2. `kafka` starts (depends on `zookeeper`)
3. `postgres` starts and becomes healthy
4. `flyway` runs migrations (depends on `postgres` health)
5. `minio` starts and becomes healthy
6. `app` starts (depends on `postgres`, `kafka`, `minio` health)

---

## 8. Build Commands

| Command | Description |
|---------|-------------|
| `docker compose build` | Build all images (Maven build inside container) |
| `docker compose up -d` | Start all services in detached mode |
| `docker compose up -d --scale app=2` | Start with 2 app instances (horizontal scaling) |
| `docker compose down` | Stop all services and remove containers |
| `docker compose down -v` | Stop and remove volumes (destroys all data) |
| `docker compose logs -f app` | Tail application logs |
| `docker compose ps` | List running services and their status |

---

## 9. Production Considerations

For production deployment beyond Docker Compose:

| Concern | Recommendation |
|---------|---------------|
| Orchestration | Kubernetes with Helm charts or Docker Swarm |
| Secrets | External secret manager (Vault, AWS Secrets Manager, Kubernetes Secrets) |
| TLS Termination | Ingress controller or reverse proxy (NGINX, Traefik) |
| Horizontal Scaling | Multiple `app` replicas behind a load balancer |
| Database | Managed PostgreSQL (RDS, Cloud SQL) with read replicas |
| Kafka | Managed Kafka (MSK, Confluent Cloud) |
| MinIO | Multi-node MinIO or S3-compatible managed storage |
| Monitoring | Prometheus + Grafana stack on dedicated infrastructure |

---

*Version: 1.0.0*
*Status: Draft*
*Last Updated: 2026-07-28*