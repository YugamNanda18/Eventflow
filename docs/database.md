# PostgreSQL Database Design & Flyway Migrations

## Key Database Tables & Foreign Key Hierarchy

```
organizations
├── users (FK -> organizations)
├── api_keys (FK -> organizations)
├── event_types (FK -> organizations)
│   └── event_schemas (FK -> event_types)
├── events (FK -> organizations, Unique: org_id + idempotency_key)
├── outbox_events (FK -> events, FK -> organizations)
├── processed_events (Unique: event_id + consumer_name)
├── webhook_endpoints (FK -> organizations)
│   ├── webhook_subscriptions (FK -> webhook_endpoints)
│   └── webhook_deliveries (FK -> webhook_endpoints, FK -> events)
│       └── delivery_attempts (FK -> webhook_deliveries)
├── dead_letter_events (FK -> events, FK -> organizations)
└── audit_logs (FK -> organizations)
```

## Critical Indexing Strategy
- `(organization_id, idempotency_key)` on `events` for fast O(1) duplicate checks.
- `(status, created_at)` on `outbox_events` for transactional outbox polling.
- `(event_id, consumer_name)` on `processed_events` for consumer deduplication.
- `(status, next_retry_at)` on `webhook_deliveries` for efficient retry polling.
