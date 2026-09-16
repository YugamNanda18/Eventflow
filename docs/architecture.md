# EventFlow Architecture & Modular Bounded Contexts

EventFlow is constructed as a **Modular Monolith** with strong bounded contexts.

## Modular Decomposition

```
com.eventflow/
├── auth/           # JWT, BCrypt, User, Role, API Keys, Scopes
├── tenant/         # Organization context, IDOR isolation
├── event/          # Ingestion, envelope model, DB idempotency check
├── outbox/         # Transactional Outbox pattern & async scheduler
├── kafka/          # Kafka Producer & Topic definitions
├── consumer/       # Kafka Consumers & Consumer Idempotency
├── webhook/        # Webhook Engine, HMAC SHA-256 signing
├── retry/          # Exponential Backoff Scheduler & Jitter
├── dlq/            # Dead Letter Queue operations & UI triggers
├── replay/         # Event Replay Engine & audit tracking
├── ratelimit/      # Redis Rate Limiting per API key
├── audit/          # Append-Only Audit Logging
├── observability/  # Actuator, Prometheus Micrometer Metrics
├── simulator/      # Dev-Only Fault Injection Engine
└── admin/          # Thymeleaf + Bootstrap 5 Dashboard Controllers
```

## Modular Boundaries & Future Extraction
Each bounded context interacts via Spring Dependency Injection and domain service interfaces. If a component (such as the `webhook` engine) needs independent horizontal scaling in the future, it can be extracted into a standalone service reading directly from the `eventflow.webhooks` Kafka topic without modifying database schemas or API contracts.
