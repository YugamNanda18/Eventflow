# Reliability Engineering & Distributed Processing Guarantees

## 1. Transactional Outbox Pattern
Avoids dual-write inconsistency between database persistence and Kafka messaging:
```
Client Request -> DB Transaction (Save Event + Save Outbox Record PENDING) -> Commit
                                      ↓
Async Outbox Publisher Scheduler -> Polls PENDING -> Emits to Kafka -> Mark PUBLISHED
```

## 2. Ingestion Idempotency
- Unique Index: `(organization_id, idempotency_key)`.
- If key exists & payload matches -> Returns existing event (200 OK, `duplicate=true`).
- If key exists & payload differs -> Returns `409 Conflict`.

## 3. Webhook Retries & Dead Letter Queue (DLQ)
- Exponential Backoff Schedule:
  - Attempt 1: Immediate
  - Attempt 2: 1 second
  - Attempt 3: 5 seconds
  - Attempt 4: 30 seconds
  - Attempt 5: 2 minutes / 10 minutes max
- After 5 failed attempts, message enters `dead_letter_events` with full exception details for manual operator replay.
