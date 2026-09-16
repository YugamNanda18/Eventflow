# EventFlow — Comprehensive Project Architecture & Step-by-Step Usage Guide

This guide explains the complete step-by-step working of **EventFlow** — a production-grade distributed event processing and webhook automation engine built with Java 21, Spring Boot 3.3, PostgreSQL, Apache Kafka, Redis, and Thymeleaf.

---

## 🎯 1. Core Problem Solved by EventFlow

In distributed microservice architectures (e.g. Stripe, Shopify, GitHub, Twilio), systems need to notify downstream services when events occur (e.g. `order.created`, `payment.succeeded`).

Building naive webhook dispatchers leads to severe production issues:
1. **Dual-write failure**: Saving an event to the DB succeeds, but sending it to Kafka/HTTP fails (or vice versa).
2. **Duplicate dispatches**: Network retries cause clients to receive the exact same event multiple times.
3. **Downstream outages**: If a client's server is down, webhooks fail without retry logic or get permanently lost.
4. **Security tampering**: Attackers forge fake HTTP webhooks if payloads are not cryptographically signed.

**EventFlow solves all of these problems through real-world backend reliability patterns.**

---

## 🔄 2. Step-by-Step Event Processing Lifecycle

```
[ Client / Producer App ]
          │
          │ 1. POST /api/v1/events (with Authorization: Bearer <API_KEY> + Idempotency Key)
          ▼
[ EventFlow API Ingestion Layer ]
          │
          │ 2. Check Redis Token Bucket Rate Limiter (100 req/min)
          │ 3. Validate Organization & API Key Hash (SHA-256)
          │ 4. Check DB Unique Constraint (Organization_ID, Idempotency_Key)
          │    ├── Existing Key + Same Payload -> Return 200 OK (Idempotent)
          │    └── Existing Key + Different Payload -> Return 409 Conflict
          │
          │ 5. Transactional Outbox Pattern (Single DB Transaction)
          │    ├── Insert into `events` table
          │    └── Insert into `outbox_events` table (Status: PENDING)
          ▼
[ Outbox Publisher Scheduler ]
          │
          │ 6. Background thread polls `outbox_events` WHERE status = 'PENDING'
          │ 7. Emits event payload to Apache Kafka topic `eventflow.events`
          │ 8. Updates outbox record status to `PUBLISHED`
          ▼
[ Kafka Consumer Engine ]
          │
          │ 9. Consumer Group listens on `eventflow.events`
          │ 10. Check `processed_events` table (Consumer Idempotency)
          │ 11. Match registered Webhook Subscriptions for eventType (e.g. `order.created`)
          ▼
[ Webhook Delivery Engine ]
          │
          │ 12. Compute HMAC SHA-256 Signature (`X-EventFlow-Signature`) using Webhook Secret Key
          │ 13. Dispatch HTTP POST to Target Webhook URL
          │     ├── HTTP 2xx Response -> Record SUCCESS in `delivery_attempts`
          │     └── HTTP 4xx/5xx Failure -> Schedule Exponential Backoff Retry (1s, 5s, 30s, 2m, 10m)
          ▼
[ Dead Letter Queue (DLQ) & Operator Intervention ]
          │
          │ 14. If max retries (5) exceeded -> Move message to `dead_letter_events` table
          │ 15. Operator inspects stack trace in Admin Dashboard
          │ 16. Operator clicks "Replay" -> Re-queues event without mutating original records
```

---

## 🔐 3. Authentication & Login Details

### Web Dashboard Login
- **Dashboard URL**: `http://localhost:8080/dashboard` (or `https://eventflow-6fi3.onrender.com/dashboard`)
- **Admin Email**: `admin@demo.eventflow`
- **Password**: `Password123!`

### REST API Authentication
All REST ingestion and management endpoints require a Bearer API Key header:
```http
Authorization: Bearer ef_live_demo1234567890abcdef123456
```

---

## 💻 4. How to Use EventFlow (Step-by-Step Practical Examples)

### Step 1: Ingest an Event (Producer Integration)

Send an HTTP POST request to `/api/v1/events`:

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
      "totalAmount": 4999,
      "currency": "USD"
    }
  }'
```

#### Response (`200 OK` or `201 Created`):
```json
{
  "eventId": "evt_a1b2c3d4e5f67890",
  "status": "RECEIVED",
  "idempotencyKey": "order-9981-created",
  "correlationId": "corr_9a8b7c6d5e4f3210",
  "occurredAt": "2026-09-16T10:35:00Z"
}
```

---

### Step 2: Register a Webhook Endpoint (Subscriber Integration)

To receive webhooks whenever an event occurs, register an endpoint via POST `/api/v1/webhooks`:

```bash
curl -X POST https://eventflow-6fi3.onrender.com/api/v1/webhooks \
  -H "Authorization: Bearer ef_live_demo1234567890abcdef123456" \
  -H "Content-Type: application/json" \
  -d '{
    "url": "https://your-api.com/webhooks/listener",
    "description": "Payment Notifications Listener",
    "eventTypes": ["order.created", "payment.succeeded"]
  }'
```

---

### Step 3: Verify Webhook Signature on Listener Server

When EventFlow dispatches a webhook to your target URL, it includes a signature header:
```http
X-EventFlow-Signature: t=1726482900,v1=5d41402abc4b2a76b9719d911017c592
```

#### Python Webhook Listener Verification Code:
```python
import hmac, hashlib

def verify_webhook_signature(payload_str, signature_header, secret_key):
    # Extract timestamp (t) and signature (v1)
    parts = dict(item.split('=') for item in signature_header.split(','))
    timestamp = parts['t']
    expected_signature = parts['v1']
    
    # Re-create signed payload string
    signed_payload = f"{timestamp}.{payload_str}".encode('utf-8')
    computed_signature = hmac.new(secret_key.encode('utf-8'), signed_payload, hashlib.sha256).hexdigest()
    
    return hmac.compare_digest(computed_signature, expected_signature)
```

---

## 📊 5. Platform Management Modules

| Module | URL Path | Description |
|---|---|---|
| **Dashboard** | `/dashboard` | System throughput, active endpoints, outbox lag, DLQ alerts. |
| **Event Explorer** | `/events` | Real-time searchable history of all ingested events. |
| **Webhooks** | `/webhooks` | Registered endpoints & full HTTP delivery attempt logs. |
| **DLQ Inspector** | `/dlq` | Captures failed deliveries after 5 retries; allows 1-click Replay or Discard. |
| **Audit Logs** | `/audit` | Append-only security audit trail of all mutations. |
| **API Keys** | `/api-keys` | Generate hashed API keys (`ef_live_...`) with fine-grained scopes. |
| **Failure Simulator** | `/simulator` | Dev sandbox to simulate 500 errors, network timeouts, or consumer crashes. |
