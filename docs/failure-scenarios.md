# EventFlow Failure Scenarios & Mitigation Strategies

| Failure Scenario | Risk / Symptom | Mitigation & Guarantees in EventFlow |
|---|---|---|
| **Database Failure during Event Ingestion** | Event cannot be stored | HTTP 500 returned to Client. Client retries with same `idempotencyKey`. |
| **Kafka Unavailability during Ingestion** | Producer cannot send to broker | **Transactional Outbox Pattern**: Event and Outbox record are saved in Postgres first. Outbox publisher retries publishing to Kafka asynchronously when broker recovers. |
| **Kafka Consumer Crash mid-processing** | Consumer crashes before completing work | Kafka consumer offset is **not committed**. Upon restart or rebalance, another consumer in the group receives the message. **Processed Event Table** prevents duplicate execution. |
| **Downstream Webhook Target Returns HTTP 500** | Webhook endpoint server error | EventFlow enters **Exponential Backoff Retry Engine** (1s, 5s, 30s, 2m, 10m). |
| **Webhook Retry Limit Exceeded (5 attempts)** | Endpoint permanently failing | Event routed to **Dead Letter Queue (DLQ)**. Admin UI flags failure with stack trace for operator inspection & replay. |
| **Duplicate Event Ingestion Request** | Network retry by client | Database unique constraint on `(organization_id, idempotency_key)`. Exact duplicate returns 200 OK without re-processing. |
