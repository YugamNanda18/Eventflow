# ⚡ EventFlow — Reliable Event Processing & Webhook Automation Platform

[![Java 21](https://img.shields.io/badge/Java-21-orange.svg?style=flat-square&logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen.svg?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![Apache Kafka](https://img.shields.io/badge/Apache%20Kafka-3.6-red.svg?style=flat-square&logo=apachekafka)](https://kafka.apache.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg?style=flat-square&logo=postgresql)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7-red.svg?style=flat-square&logo=redis)](https://redis.io/)
[![Docker](https://img.shields.io/badge/Docker-Enabled-blue.svg?style=flat-square&logo=docker)](https://www.docker.com/)
[![Live Demo](https://img.shields.io/badge/Render-Live%20Demo-success?style=flat-square&logo=render)](https://eventflow-6fi3.onrender.com/dashboard)

**EventFlow** is an enterprise-grade, distributed backend platform engineered to guarantee **100% reliable event ingestion, asynchronous queuing, and webhook dispatching** between microservice producers (payments, e-commerce, IoT) and downstream consumer subscribers.

It replaces fragile HTTP webhook dispatches with industrial-grade backend resilience patterns: **Transactional Outbox**, **Database-Backed Idempotency**, **HMAC SHA-256 Cryptographic Signatures**, **Exponential Backoff Retries**, and a **Dead Letter Queue (DLQ) Inspector**.

---

## 🌐 Live Demo & Instant Access

- 🚀 **Live Dashboard**: [https://eventflow-6fi3.onrender.com/dashboard](https://eventflow-6fi3.onrender.com/dashboard)
- 📖 **OpenAPI / Swagger Specs**: [https://eventflow-6fi3.onrender.com/swagger-ui.html](https://eventflow-6fi3.onrender.com/swagger-ui.html)

### 🔑 Pre-seeded Demo Credentials
- **Admin**: `admin@demo.eventflow` / `Password123!`
- **Developer**: `developer@demo.eventflow` / `Password123!`
- **Operator**: `operator@demo.eventflow` / `Password123!`
- **Demo API Key**: `ef_live_demo1234567890abcdef123456`

---

## 🏗️ Architecture & System Lifecycle

```
[ Producer Service ]
         │
         │ 1. POST /api/v1/events (Bearer Token + Idempotency Key)
         ▼
[ EventFlow API Engine ]
         ├── 2. Redis Token Bucket Rate Limiting (100 req/min/key)
         ├── 3. Organization Scope & API Key Hash Verification (SHA-256)
         └── 4. DB Idempotency Check (Unique Constraint: Org_ID + Key)
         │
         │ 5. Transactional Outbox Pattern (Atomic DB Transaction)
         ├── Save `EventEntity`
         └── Save `OutboxEvent` (Status: PENDING)
         ▼
[ Outbox Publisher Scheduler ]
         └── Background thread polls PENDING outbox & emits to Kafka
         ▼
[ Apache Kafka Messaging ]
         └── Topic: `eventflow.events` (3 Partitions, Key-Ordered)
         ▼
[ Consumer & Webhook Engine ]
         ├── 6. Consumer Idempotency check (`processed_events` table)
         ├── 7. Sign Payload with HMAC SHA-256 (`X-EventFlow-Signature`)
         └── 8. HTTP POST Dispatch -> Target Webhook URL
         │
    ┌────┴────────────────────────┐
    ▼                             ▼
[ 2xx SUCCESS ]          [ 4xx/5xx Failure ]
Status = SUCCESS         Exponential Backoff Retries (1s ➔ 5s ➔ 30s ➔ 2m ➔ 10m)
                                  │
                           [ Max Retries (5) Exceeded ]
                                  │
                           Dead Letter Queue (DLQ Inspector)
```

---

## ✨ Key Technical Features

| Feature | Enterprise Pattern | Implementation Detail |
|---|---|---|
| ⚡ **Dual-Write Protection** | **Transactional Outbox** | Atomically writes `events` and `outbox_events` in 1 DB transaction before Kafka push. |
| 🛡️ **Zero Duplicates** | **DB Idempotency** | Composite unique index `(organization_id, idempotency_key)`. Identical payloads return `200 OK`. |
| 🔒 **Webhook Security** | **HMAC SHA-256 Signatures** | Header `X-EventFlow-Signature: t=<time>,v1=<hash>` protects against tampering & replay. |
| 🔁 **Fault Resilience** | **Exponential Backoff** | Automated retry schedule (1s, 5s, 30s, 2m, 10m) with jitter for failed endpoints. |
| ⚠️ **Failure Recovery** | **DLQ & Event Replay** | Inspector UI captures failed events after 5 attempts; allows 1-click operator replay. |
| 🚀 **Rate Limiting** | **Redis Sliding Window** | Token Bucket rate limiter enforcing 100 req/min per API key returning `HTTP 429`. |
| 📊 **Observability** | **Prometheus & Actuator** | Custom metrics (`event_ingestion_total`, `webhook_latency_ms`, `dlq_unresolved_total`). |
| 🏢 **Multi-Tenancy** | **Tenant Context** | ThreadLocal `TenantContext` enforcing strict organizational data boundary checks. |

---

## 🚀 Quickstart & Local Docker Deployment

### 1. Run Complete Stack (Postgres, Kafka, Redis, EventFlow)
```bash
git clone https://github.com/YugamNanda18/eventflow.git
cd eventflow
docker compose up --build -d
```

### 2. Access Local Services
- **Dashboard**: `http://localhost:8080/dashboard`
- **OpenAPI UI**: `http://localhost:8080/swagger-ui.html`
- **Prometheus Metrics**: `http://localhost:8080/actuator/prometheus`

---

## 📡 API Ingestion & Usage Examples

### 1. Ingest an Event (Producer Service)
```bash
curl -X POST https://eventflow-6fi3.onrender.com/api/v1/events \
  -H "Authorization: Bearer ef_live_demo1234567890abcdef123456" \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "order.created",
    "eventVersion": "1.0",
    "source": "checkout-service",
    "idempotencyKey": "order-9981-created",
    "payload": {
      "orderId": "ORD-9981",
      "customerEmail": "customer@example.com",
      "amount": 4999
    }
  }'
```

### 2. Register Webhook Target (Subscriber Service)
```bash
curl -X POST https://eventflow-6fi3.onrender.com/api/v1/webhooks \
  -H "Authorization: Bearer ef_live_demo1234567890abcdef123456" \
  -H "Content-Type: application/json" \
  -d '{
    "url": "https://your-server.com/api/v1/webhooks/listener",
    "description": "Payment Notifications Listener",
    "eventTypes": ["order.created", "payment.succeeded"]
  }'
```

---

## 📑 Deep-Dive Documentation Index

- [📘 Comprehensive Project & Usage Guide](docs/PROJECT_GUIDE.md)
- [🏛️ System Architecture & Bounded Modules](docs/architecture.md)
- [📡 Kafka Topic Partitioning & Ordering](docs/kafka.md)
- [🗄️ Database Schemas & Flyway Migrations](docs/database.md)
- [🔐 Security & API Key SHA-256 Hashing](docs/security.md)
- [⚙️ Reliability & Outbox Pattern Deep-Dive](docs/reliability.md)
- [📝 API Reference Specifications](docs/api.md)
- [☁️ Zero-Error Cloud Deployment Guide](docs/deployment.md)
- [🛠️ Failure Scenarios & Mitigation](docs/failure-scenarios.md)
- [🎓 Backend Engineering Interview Guide](docs/interview-guide.md)

---

## 📄 License
This project is licensed under the MIT License.
