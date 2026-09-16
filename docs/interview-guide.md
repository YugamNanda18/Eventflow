# EventFlow Technical Interview Guide

This guide equips you with deep technical responses for backend and distributed systems architectural interviews based on EventFlow.

---

### Q1: What problem does EventFlow solve?
**Answer**: In microservices and event-driven architectures, reliable delivery between event producers and external downstream consumers (webhooks, audit systems, notifications) is vulnerable to dual-write race conditions, consumer crashes, network dropouts, and rate limits. EventFlow provides a reliable event-processing and webhook automation platform guaranteeing database-backed idempotency, outbox publishing, HMAC signature security, exponential backoff retries, and dead-letter queue operations.

---

### Q2: Why Apache Kafka instead of RabbitMQ or standard HTTP calls?
**Answer**: Direct HTTP calls during event ingestion couple the producer to consumer latency and outages. Apache Kafka acts as an append-only distributed commit log with high-throughput partitioning, persistence, and consumer group rebalancing. While RabbitMQ excels at complex queue routing, Kafka’s log-centric model allows event replay, strict partition-key ordering (`tenantId:aggregateId`), and independent scaling of consumer groups.

---

### Q3: How does the Transactional Outbox Pattern prevent dual-write loss?
**Answer**: In traditional code:
```java
// Vulnerable to crash between save & send!
eventRepository.save(event);
kafkaTemplate.send("events", event);
```
If the app crashes or Kafka is down after `eventRepository.save()`, the event is persisted in DB but never published to Kafka.
In EventFlow, `EventEntity` and `OutboxEvent` are committed in a **single DB transaction**. An async scheduler (`OutboxPublisherScheduler`) polls pending outbox records and publishes them to Kafka with retries, eliminating dual-write inconsistency.

---

### Q4: How does EventFlow handle idempotency at both ingestion and consumption layers?
**Answer**:
1. **Ingestion Layer**: Unique database constraint on `(organization_id, idempotency_key)`. If a duplicate request arrives with the same payload, EventFlow returns `200 OK` with existing event details. If the payload differs, it returns `409 Conflict`.
2. **Consumer Layer**: Kafka consumers insert a record into `processed_events` with unique constraint `(event_id, consumer_name)` before taking action. If a duplicate message is delivered by Kafka, the consumer skips duplicate execution.

---

### Q5: How are webhooks secured against tampering and replay attacks?
**Answer**: Webhooks are signed using **HMAC SHA-256**. The header `X-EventFlow-Signature: t=<timestamp>,v1=<signature>` is computed over `timestamp + "." + rawBody` using the endpoint's secret key. Downstream receivers verify the signature and check timestamp tolerance (e.g., 5 minutes) to block replay attacks.

---

### Q6: How would you convert this Modular Monolith into microservices?
**Answer**: Because module boundaries (`event`, `outbox`, `webhook`, `dlq`, `audit`) are clean and decoupled:
1. Extract `WebhookEngine` into an independent microservice reading directly from `eventflow.webhooks` Kafka topic.
2. Extract `IngestionService` into an API Gateway / Ingestion service reading `eventflow.events`.
3. Database schemas can be split into separate tenant/event and webhook databases without breaking domain logic.
