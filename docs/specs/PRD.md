# InfluencerAPP - Product Requirements Document (PRD)

## 1. Executive Summary
InfluencerAPP is a Spring Boot REST service that automates YouTube content publishing. Users upload videos through a REST API; the system generates AI-powered titles and descriptions using Google Gemini (free tier), uploads the video to a pre-registered YouTube channel, and notifies subscribers via Apache Kafka. The platform supports multi-tenancy: each authenticated user manages their own YouTube channels.

## 2. Problem Statement
Content creators and social media managers spend significant time manually uploading videos to YouTube, crafting optimized titles/descriptions, and managing metadata across multiple channels. This process is repetitive, error-prone, and doesn't scale when managing multiple channels or high upload volumes.

## 3. Solution Overview
A backend service that acts as an intelligent video publishing pipeline:
- Accept video uploads via REST API
- Automatically generate engaging titles and descriptions using AI
- Upload directly to the user's designated YouTube channel
- Notify downstream subscribers via event streaming (Kafka)

## 4. Target Audience
- **Primary**: Solo content creators managing 1-5 YouTube channels
- **Secondary**: Small social media agencies managing multiple client channels
- **Technical Level**: Non-technical users via API integration, or developers building on top of the service

## 5. Features

### 5.1 MVP Features (v1.0)
| Feature | Description | Priority |
|---------|-------------|----------|
| User Registration & Auth | JWT-based authentication | Must Have |
| Channel Registration | OAuth2 flow to link YouTube channels | Must Have |
| Video Upload | Multipart upload via REST API (max 100MB) | Must Have |
| AI Metadata Generation | Gemini-powered title + description | Must Have |
| YouTube Upload | Direct upload to linked channel | Must Have |
| Status Tracking | Poll video processing status | Must Have |
| Kafka Notifications | Event-driven notifications on publish | Must Have |

### 5.2 Post-MVP Features (v2.0+)
| Feature | Description | Priority |
|---------|-------------|----------|
| Batch Upload | Upload multiple videos in one request | Nice to Have |
| Thumbnail Generation | AI-generated thumbnails | Nice to Have |
| Scheduling | Schedule video publication | Nice to Have |
| Analytics Dashboard | Views, engagement metrics | Nice to Have |
| Multi-language Support | i18n for metadata generation | Nice to Have |
| Webhook Callbacks | Real-time status updates to client | Nice to Have |
| Video Transcoding | Auto-convert formats for YouTube | Nice to Have |

## 6. User Stories

### Authentication & Channels
- **US-001**: As a new user, I want to register an account so I can access the service
- **US-002**: As a registered user, I want to log in and receive a JWT so I can authenticate subsequent requests
- **US-003**: As an authenticated user, I want to link my YouTube channel via OAuth2 so the system can upload to it
- **US-004**: As an authenticated user, I want to view my linked channels so I know which ones are available

### Video Upload & Processing
- **US-005**: As an authenticated user, I want to upload a video file via API so it can be processed
- **US-006**: As an authenticated user, I want the system to automatically generate a title and description for my video so I don't have to write them manually
- **US-007**: As an authenticated user, I want the system to upload my video to my selected YouTube channel so it's published
- **US-008**: As an authenticated user, I want to check the status of my upload so I know if it succeeded or failed

### Notifications & Events
- **US-009**: As a system administrator, I want to subscribe to Kafka events so I can trigger downstream workflows
- **US-010**: As an authenticated user, I want to receive a notification when my video is published so I can share it

## 7. Technical Requirements

### 7.1 Architecture
- **Pattern**: Hexagonal Architecture (Ports & Adapters)
- **Language**: Java 17+
- **Framework**: Spring Boot 3.x
- **Build Tool**: Maven (multi-module structure)
- **Boilerplate Reduction**: Lombok annotations required (`@Data`, `@Builder`, `@Slf4j`, `@Value`, `@With`, `@RequiredArgsConstructor`). Manual getters/setters/constructors are forbidden.
- **Code Quality**: Clean Code principles enforced — single responsibility, meaningful names, small functions, no dead code, no commented-out blocks. Cyclomatic complexity must remain low.
- **Database**: PostgreSQL
- **Message Broker**: Apache Kafka
- **Storage**: MinIO (object storage)
- **AI**: Google Generative AI (Gemini) — free tier

### 7.2 API Design
- RESTful API with JSON payloads
- JWT Bearer authentication
- Multipart/form-data for video uploads
- OpenAPI 3.0 documentation (SpringDoc)

### 7.3 Non-Functional Requirements
- **Availability**: 99.5% uptime (excluding third-party dependencies)
- **Throughput**: Support 10 concurrent uploads per tenant
- **Latency**: <5s for API responses (excluding upload/processing time)
- **File Size Limit**: 100MB per video (configurable)
- **Security**: OAuth2 tokens encrypted at rest, HTTPS only in production
- **Code Quality**: Clean Code principles enforced — meaningful names, small classes/methods, no dead code. Lombok used consistently to reduce boilerplate. No manual getters/setters/constructors.

### 7.4 Integrations
| Integration | Purpose | Auth Method |
|-------------|---------|-------------|
| Google Gemini API | AI text generation | API Key |
| YouTube Data API v3 | Video upload | OAuth2 (per user) |
| Apache Kafka | Event streaming | SASL/SSL |
| MinIO | Object storage | Access Key / Secret Key |

## 8. Success Metrics

### Business Metrics
- **Upload Success Rate**: >95% of uploads complete without manual intervention
- **Processing Time**: <2 minutes from upload to YouTube publish (excluding YouTube processing time)
- **User Retention**: >80% of users link at least 2 channels

### Technical Metrics
- **API Latency (p95)**: <500ms for non-upload endpoints
- **Error Rate**: <1% of requests return 5xx
- **Kafka Lag**: <10 seconds between VIDEO_RECEIVED and VIDEO_PUBLISHED events
- **Code Coverage**: 90%+ for domain and application layers
- **Static Analysis**: No critical SonarQube issues; Lombok usage consistent across codebase

## 9. Project Structure

```
influencerAPP/
├── pom.xml                          # Parent POM (dependency management)
├── docker-compose.yml
├── Dockerfile
├── README.md
├── arq/
│   └── architecture.md
├── docs/
│   └── specs/
│       ├── constitution.md
│       └── PRD.md
├── domain/                          # Maven module: pure domain, no framework deps
│   ├── pom.xml
│   └── src/main/java/com/influencerapp/domain/
│       ├── model/
│       ├── port/
│       │   ├── inbound/
│       │   └── outbound/
│       └── exception/
├── application/                     # Maven module: use cases, orchestration
│   ├── pom.xml
│   └── src/main/java/com/influencerapp/application/
│       ├── service/
│       ├── event/
│       └── dto/
└── infrastructure/                  # Maven module: adapters, config, repos
    ├── pom.xml
    └── src/main/java/com/influencerapp/infrastructure/
        ├── adapter/
        │   ├── http/
        │   ├── storage/
        │   ├── ai/
        │   ├── youtube/
        │   ├── kafka/
        │   ├── security/
        │   └── monitoring/
        ├── config/
        ├── repository/
        └── entity/
```

## 10. Implementation Roadmap

### Phase 1: Foundation (Week 1-2)
- [ ] Project scaffolding (Spring Boot, Maven, directory structure)
- [ ] Database setup (PostgreSQL, Flyway migrations)
- [ ] Domain layer (entities, ports)
- [ ] Security layer (JWT auth)

### Phase 2: Core Features (Week 3-4)
- [ ] User registration/login endpoints
- [ ] YouTube OAuth2 channel registration flow
- [ ] Gemini adapter for title/description generation
- [ ] MinIO storage adapter

### Phase 3: Upload Pipeline (Week 5-6)
- [ ] YouTube upload adapter
- [ ] Kafka producer/consumer setup
- [ ] Video processing orchestration
- [ ] REST controllers

### Phase 4: Production Readiness (Week 7-8)
- [ ] Docker compose (app, postgres, kafka, zookeeper, minio)
- [ ] Error handling, retries, circuit breakers
- [ ] Comprehensive tests (unit, integration)
- [ ] Documentation (README, API docs)

## 11. Risks & Mitigations

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| YouTube OAuth2 setup complexity | High | Medium | Provide detailed setup guide for Google Cloud Console |
| YouTube quota exhaustion | High | Low | Implement quota monitoring, circuit breaker, user notifications |
| Gemini free tier rate limits | Medium | Medium | Implement request queueing, fallback to placeholder text |
| Large file memory pressure | High | Medium | Use streaming uploads, never load entire file into memory |
| Token security | High | Low | Encrypt tokens at rest, use JCE for encryption keys |
| Kafka unavailability | Medium | Low | Local event buffering with retry on reconnect |

## 12. Open Questions
- Should we support video transcoding before upload to ensure YouTube compatibility?
- Do we need webhook support for real-time client notifications in addition to Kafka?
- What is the expected retention policy for video files in object storage?
