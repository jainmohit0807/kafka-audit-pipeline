# Kafka Audit Pipeline

[![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3-6DB33F?logo=springboot)](https://spring.io/projects/spring-boot)
[![Kafka](https://img.shields.io/badge/Apache_Kafka-7.7-231F20?logo=apachekafka)](https://kafka.apache.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-316192?logo=postgresql)](https://www.postgresql.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

A production-grade **event-driven audit pipeline** built with Spring Boot 3.3, Apache Kafka, and PostgreSQL. Features REST-based event ingestion, Kafka-backed async processing, yearly partitioned storage, dead letter queue handling, and a configurable 7-year retention policy with automatic partition lifecycle management.

> **Inspired by compliance pipelines processing 500K+ audit events/day at enterprise scale.**

---

## Key Features

| Feature | Description |
|---|---|
| **REST Producer** | Validated REST API to ingest audit events, publish to Kafka with idempotent producer guarantees. |
| **Kafka Consumer** | Concurrent listener with manual offset management and exactly-once semantics (read_committed). |
| **Partitioned Storage** | Audit records tagged by year for PostgreSQL range partitioning. Enables efficient time-range queries and partition drops. |
| **Dead Letter Queue** | Failed messages routed to DLQ topic after 3 retries (fixed backoff). DLQ listener persists failures for investigation. |
| **7-Year Retention** | Configurable retention scheduler drops partitions older than N years. Runs on cron (default: Jan 1 midnight). |
| **Observability** | Spring Actuator endpoints (health, metrics, Prometheus). Structured logging with Kafka metadata. |
| **API Documentation** | Auto-generated OpenAPI 3.0 / Swagger UI at `/api/swagger-ui.html`. |

---

## Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│                         Client / Service                          │
│                    POST /v1/audit-events                          │
└──────────────────────────┬───────────────────────────────────────┘
                           │
┌──────────────────────────▼───────────────────────────────────────┐
│  REST Controller → Validate → AuditEventPublisher                 │
├──────────────────────────┬───────────────────────────────────────┤
│                          │ KafkaTemplate.send()                   │
│                          ▼                                        │
│  ┌─────────────────────────────────────────────────────────┐     │
│  │              Apache Kafka (audit-events topic)            │     │
│  │  Partitions: 6 | Replication: configurable               │     │
│  └────────────┬────────────────────────┬────────────────────┘     │
│               │                        │ (on failure × 3)         │
│               ▼                        ▼                          │
│  ┌─────────────────────┐  ┌──────────────────────────────┐      │
│  │  AuditEventListener  │  │  Dead Letter Queue (DLQ)      │      │
│  │  (consumer group)    │  │  audit-events-dlq topic        │      │
│  └──────────┬──────────┘  └──────────────┬───────────────┘      │
│             │                             │                       │
│             ▼                             ▼                       │
│  ┌─────────────────────────────────────────────────────────┐     │
│  │  AuditPersistenceService → PostgreSQL (partitioned)       │     │
│  │  partition_year = YEAR(NOW())                             │     │
│  └─────────────────────────────────────────────────────────┘     │
│                                                                    │
│  ┌─────────────────────────────────────────────────────────┐     │
│  │  RetentionScheduler (cron) → DROP partitions > 7 years    │     │
│  └─────────────────────────────────────────────────────────┘     │
└──────────────────────────────────────────────────────────────────┘
```

For detailed sequence diagrams and partition strategy, see [docs/architecture.md](docs/architecture.md).

---

## Quick Start

### Prerequisites
- Docker & Docker Compose
- Java 21 (for local development)

### Run with Docker (3 commands)

```bash
git clone https://github.com/jainmohit0807/kafka-audit-pipeline.git
cd kafka-audit-pipeline
docker compose up -d
```

The API is available at `http://localhost:8080/api`.
Swagger UI: `http://localhost:8080/api/swagger-ui.html`.

### Run Locally

```bash
# Start dependencies (Postgres + Kafka + Zookeeper)
docker compose up -d postgres zookeeper kafka

# Run with default config
./mvnw spring-boot:run
```

**Without Docker** (H2, no Kafka broker): `./mvnw spring-boot:run -Dspring-boot.run.profiles=local`

### IntelliJ IDEA

1. **Open** the project folder (the directory that contains `pom.xml`).
2. **Maven** tool window → **Reload All Maven Projects**.
3. **Settings → Build Tools → Maven** → **User settings file**: Override → point to `.mvn/settings-public.xml`.
4. **Run** with VM option `-Dspring.profiles.active=local` for H2 mode, or start Kafka + Postgres via Docker Compose for full mode.

---

## API Reference

### Audit Events

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/v1/audit-events` | Publish an audit event to Kafka |
| GET | `/v1/audit-events` | Query persisted audit records (paginated, filterable by source/action) |
| GET | `/v1/audit-events/count` | Count total persisted audit records |

### Example: Publish an Audit Event

```bash
curl -X POST http://localhost:8080/api/v1/audit-events \
  -H "Content-Type: application/json" \
  -d '{
    "source": "user-service",
    "action": "LOGIN",
    "actorId": "user-123",
    "actorEmail": "user@example.com",
    "resourceType": "session",
    "resourceId": "sess-456",
    "ipAddress": "192.168.1.100",
    "outcome": "SUCCESS",
    "metadata": { "browser": "Chrome", "os": "macOS" }
  }'
```

Response:
```json
{
  "eventId": "a1b2c3d4-...",
  "topic": "audit-events",
  "partition": 2,
  "offset": 42,
  "publishedAt": "2026-04-08T10:30:00Z",
  "status": "ACCEPTED"
}
```

A [Postman collection](postman/kafka-audit-pipeline.postman_collection.json) is included for interactive testing.

---

## Retention Policy

The pipeline enforces a configurable retention policy for compliance:

```yaml
audit:
  retention:
    years-to-retain: 7                          # Keep 7 years of audit data
    partition-check-cron: "0 0 0 1 1 *"         # Run Jan 1 midnight
    auto-create-partitions: true                 # Create yearly partitions ahead
    auto-drop-expired-partitions: true           # Drop partitions older than retention
```

| Setting | Default | Description |
|---|---|---|
| `years-to-retain` | 7 | Number of years to keep audit records |
| `partition-check-cron` | Jan 1, midnight | When to check for expired partitions |
| `auto-drop-expired-partitions` | true | Automatically drop old partitions |

---

## Dead Letter Queue

Failed messages are routed to the DLQ after 3 retry attempts:

```
audit-events → (fail × 3) → audit-events-dlq → DeadLetterService → PostgreSQL (source=DLQ)
```

DLQ records are persisted with `source=DLQ` and `action=DEAD_LETTER` for investigation and replay.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Runtime | Java 21, Spring Boot 3.3 |
| Messaging | Apache Kafka (Confluent 7.7) |
| Database | PostgreSQL 16 (partitioned tables) |
| ORM | Spring Data JPA / Hibernate 6 |
| Migration | Flyway |
| API Docs | SpringDoc OpenAPI 2.6 |
| Containerization | Docker multi-stage build |
| CI/CD | GitHub Actions |
| Testing | JUnit 5, Mockito, MockMvc, spring-kafka-test |

---

## Project Structure

```
src/main/java/com/mohitjain/audit/
├── config/           # Kafka topics, retention properties, OpenAPI, JPA auditing
├── producer/
│   ├── controller/   # REST API for publishing audit events
│   ├── dto/          # Request/response DTOs with validation
│   └── service/      # KafkaTemplate-based event publisher
├── consumer/
│   ├── listener/     # Kafka consumer with manual ack + DLQ listener
│   ├── service/      # Persistence service, dead letter service
│   ├── entity/       # AuditRecord JPA entity (partitioned by year)
│   └── repository/   # Spring Data JPA repository with partition queries
├── retention/        # Scheduled retention policy enforcement
└── exception/        # RFC 7807 global exception handler
```

---

## Testing

```bash
# Unit tests
./mvnw test

# Integration tests (requires Docker for Testcontainers)
./mvnw verify

# Test with coverage report
./mvnw test jacoco:report
```

---

## Roadmap

- [ ] Kafka Connect sink connector for direct PostgreSQL ingestion
- [ ] Avro schema registry for event schema evolution
- [ ] Grafana dashboard with Kafka lag and throughput metrics
- [ ] Event replay CLI tool from DLQ to main topic
- [ ] Kubernetes Helm chart with Strimzi Kafka operator
- [ ] Multi-region Kafka MirrorMaker replication

---

## Contributing

Contributions are welcome! Please open an issue first to discuss proposed changes.

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.

---

## Author

**Mohit Jain** — Tech Lead | Backend Engineering
[GitHub](https://github.com/jainmohit0807) · [LinkedIn](https://linkedin.com/in/mohit-jain-264296115) · [Blog](https://jainmohit0807.hashnode.dev)
