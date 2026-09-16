# Apache Kafka Architecture & Partitioning Strategy

## Core Kafka Topics

1. `eventflow.events`: Primary event stream published by Outbox Publisher.
2. `eventflow.webhooks`: Internal topic for webhook dispatch tasks.
3. `eventflow.notifications`: Downstream notification events.
4. `eventflow.audit`: Event stream for audit logging.
5. `eventflow.retry`: Scheduled retries.
6. `eventflow.dlq`: Unprocessable messages.

## Partitioning Strategy
- Partition Key Format: `tenantId + ":" + source` or `tenantId + ":" + aggregateId`.
- Guarantees strict event ordering per aggregate within a single partition while distributing load across consumer group workers.

## Delivery Guarantees
- **Producer**: Idempotent producer enabled (`acks=all`, `retries=3`).
- **Broker**: Topic replication factor 1 (configurable for multi-broker clusters).
- **Consumer**: At-least-once delivery with **Application-Level Consumer Idempotency** via `processed_events` unique constraints.
