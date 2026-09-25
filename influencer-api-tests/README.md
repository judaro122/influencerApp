# InfluencerAPP — Automated API Test Suite

Automated end-to-end and integration testing application for the InfluencerAPP REST API, built with **Java 17**, **REST Assured**, **JUnit 5**, **AssertJ**, **Awaitility**, and **Allure Reporting**.

---

## Features
- **Dynamic Multi-Environment Support:** Run against remote servers (`http://192.168.100.10:8080`) or local ephemeral CI/CD environments (`http://localhost:8080`) without changing code.
- **Full Endpoint Coverage:** Smoke, Authentication & Registration, Channel Linking, Multipart Video Uploads, Idempotency Caching, and Async Video Publishing Lifecycle.
- **Tenant Isolation Testing:** Verifies strict data separation between multiple tenants.
- **Async Polling:** Uses Awaitility for non-blocking status verification (`RECEIVED` -> `PROCESSING` -> `PUBLISHED`).
- **Allure Reporting:** Generates detailed visual reports with full request/response payloads and execution timelines.

---

## Project Structure
```
influencer-api-tests/
├── pom.xml
├── src/test/
│   ├── java/com/influencerapp/test/
│   │   ├── config/TestConfig.java             # Dynamic environment resolver
│   │   ├── client/                            # API client wrappers (Auth, Channel, Video, Health)
│   │   ├── model/                             # Request/Response/ProblemDetail DTOs
│   │   ├── utils/                             # TestDataGenerator, MediaFileProvider, PollingUtils
│   │   ├── BaseApiTest.java                   # Setup, logging, authentication helpers
│   │   └── suites/
│   │       ├── HealthSmokeTest.java           # @Tag("smoke")
│   │       ├── AuthenticationTest.java        # @Tag("auth")
│   │       ├── SecurityTenantIsolationTest.java # @Tag("security")
│   │       ├── ChannelManagementTest.java     # @Tag("channels")
│   │       ├── VideoUploadTest.java           # @Tag("videos")
│   │       └── VideoPublishingE2ETest.java    # @Tag("e2e")
│   └── resources/
│       ├── application-test.properties        # Default target URL & timeouts
│       └── logback-test.xml
```

---

## Execution Commands

### 1. Run against Remote Deployed Server (e.g. 192.168.100.10:8080)
```bash
# Run all tests
mvn clean test -Dtarget.base.url="http://192.168.100.10:8080"

# Run only Smoke tests
mvn test -Dgroups="smoke" -Dtarget.base.url="http://192.168.100.10:8080"

# Run only Regression tests
mvn test -Dgroups="regression" -Dtarget.base.url="http://192.168.100.10:8080"

# Run End-to-End Publishing Pipeline tests
mvn test -Dgroups="e2e" -Dtarget.base.url="http://192.168.100.10:8080"
```

### 2. Run in CI/CD (Jenkins / Ephemeral Environment)
In Jenkins, the root `Jenkinsfile` automatically executes:
```bash
# Start stack
docker compose up -d --build

# Run tests targeting localhost
mvn clean test -Dtarget.base.url="http://localhost:8080"

# Clean up
docker compose down -v
```

### 3. Generate Allure Report
```bash
mvn allure:serve
```
