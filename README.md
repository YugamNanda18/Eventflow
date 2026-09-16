# EventFlow — Reliable Event Processing & Webhook Automation Platform

EventFlow is a production-oriented distributed backend platform engineered with Java 21, Spring Boot 3.x, Apache Kafka, PostgreSQL, Redis, and Thymeleaf + Bootstrap 5.

It solves the core reliability challenges of event-driven architectures between producers (e.g. payment services, order systems, IoT telemetry) and downstream webhooks/consumers:
- Dual-write race conditions (solved with **Transactional Outbox Pattern**).
- Duplicate event submissions (solved with **Database-Backed Idempotency**).
- Consumer re-processing (solved with **Consumer Idempotency Tables**).
- Intermittent HTTP failures & network outages (solved with **Exponential Backoff Retries + Jitter**).
- Unprocessable payloads (solved with **Dead Letter Queue (DLQ)**).
- Webhook authenticity & tampering (solved with **HMAC SHA-256 Signatures**).
- Operator manual intervention & replay (solved with **Audit-Logged Event & DLQ Replay**).

---

## 🚀 Key Features & Architectural Philosophy

1. **Modular Monolith Architecture**: Bounded modules (`auth`, `tenant`, `event`, `outbox`, `kafka`, `consumer`, `webhook`, `retry`, `dlq`, `replay`, `ratelimit`, `audit`, `observability`, `admin`, `simulator`, `demowebhook`).
2. **Multi-Tenant Isolation**: Every record is scoped to an `Organization`. Explicit IDOR protection at service & JPA repository layers.
3. **Database-Backed Idempotency**: DB unique constraint on `(organization_id, idempotency_key)`. Identical payload re-submissions return `200 OK` safely; mismatched payload on the same key returns deterministic `409 Conflict`.
4. **Transactional Outbox Pattern**: Atomic database transaction saves the `EventEntity` and `OutboxEvent`. A background scheduler polls `PENDING` outbox records and emits to Kafka.
5. **Consumer Idempotency**: Consumers check `processed_events` table `(event_id, consumer_name)` before processing.
6. **Webhook Engine with HMAC Signatures**: Generates `X-EventFlow-Signature` (`t=<timestamp>,v1=HMAC-SHA256(...)`) header. Supports timestamp tolerance to prevent replay attacks.
7. **Exponential Backoff Retries**: Configurable schedule (1s, 5s, 30s, 2m, 10m).
8. **Dead Letter Queue (DLQ)**: Failed deliveries after max retries enter DLQ. Operators can inspect, retry, or discard from the Admin UI.
9. **Event Replay Engine**: Re-queues events with correlation sub-tracking without mutating historical records.
10. **Redis Rate Limiting**: Token bucket rate limiter (100 req/min/API key) returning `HTTP 429`.
11. **Append-Only Audit Log**: Tracks all user and system mutations (`EVENT_CREATED`, `DLQ_RETRIED`, `REPLAY_STARTED`, etc.).
12. **Observability**: Spring Boot Actuator + Micrometer Prometheus counters (`event_ingestion_total`, `webhook_delivery_latency`, `dlq_total`, etc.).
13. **Dev Failure Simulator**: Injects 500 errors, timeouts, or consumer crashes to demonstrate system resilience.
14. **Demo Webhook Receiver**: Built-in endpoint (`/api/v1/demo-webhook`) to demonstrate end-to-end delivery locally.

---

## 🛠️ Technology Stack

| Component | Technology |
|---|---|
| **Language** | Java 21 |
| **Framework** | Spring Boot 3.3.4 (Web, Security, Data JPA, Actuator, Thymeleaf) |
| **Database** | PostgreSQL 16 (Flyway Migrations) |
| **Messaging** | Apache Kafka 3.6 |
| **Cache & Rate Limiter** | Redis 7 |
| **Security** | Spring Security 6, JWT, BCrypt, API Key Hashing |
| **API Documentation** | OpenAPI 3 / Swagger (`/swagger-ui.html`) |
| **Metrics** | Micrometer Prometheus (`/actuator/prometheus`) |
| **Containers** | Docker & Docker Compose |
| **Testing** | JUnit 5, Mockito, Spring Boot Test, Testcontainers |

---

## ⚡ Quickstart (Local Docker Setup)

```bash
# 1. Clone repository & build full stack
docker compose up --build

# 2. Access Admin Dashboard
open http://localhost:8080/dashboard

# 3. Access OpenAPI / Swagger Specs
open http://localhost:8080/swagger-ui.html
```

### Demo Credentials
- **Admin**: `admin@demo.eventflow` / `Password123!`
- **Developer**: `developer@demo.eventflow` / `Password123!`
- **Operator**: `operator@demo.eventflow` / `Password123!`
- **Demo API Key**: `ef_live_demo1234567890abcdef123456`

---

## 📡 API Ingestion Example

```bash
curl -X POST http://localhost:8080/api/v1/events \
  -H "Authorization: Bearer ef_live_demo1234567890abcdef123456" \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "order.created",
    "eventVersion": "1.0",
    "source": "commerce-service",
    "idempotencyKey": "order-98231-created",
    "payload": {
      "orderId": "ORD-98231",
      "customerId": "CUS-81",
      "amount": 2499
    }
  }'
```

---

## 📑 Documentation Index
- [Architecture & Design](docs/architecture.md)
- [Kafka Design & Partitioning](docs/kafka.md)
- [Database Schemas & Flyway](docs/database.md)
- [Security & API Key Hashing](docs/security.md)
- [Reliability & Outbox Pattern](docs/reliability.md)
- [API Reference Specs](docs/api.md)
- [Deployment Guide](docs/deployment.md)
- [Failure Scenarios & Mitigation](docs/failure-scenarios.md)
- [Backend Interview Guide](docs/interview-guide.md)
