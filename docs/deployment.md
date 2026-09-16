# EventFlow Deployment & Operational Guide

This document provides complete, hassle-free instructions for deploying **EventFlow** to local, staging, and cloud production environments without facing CORS, routing, dynamic path, or configuration errors.

---

## 1. Zero-CORS & Zero-Routing Error Guarantee

### CORS Configuration
EventFlow includes an explicit Spring Security `CorsConfigurationSource` (`SecurityConfig.java`) configured with `AllowedOriginPattern("*")`, `AllowedMethods("*")`, and `AllowedHeaders("*")`. This ensures:
- Full support for cross-origin REST API requests from any frontend (React, Vue, Next.js, Angular, Flutter, Postman).
- Automatic handling of browser `OPTIONS` pre-flight requests with standard `200 OK` responses.

### Single-Origin Routing (SPA & Thymeleaf Dashboard)
- All UI routes (`/dashboard`, `/events`, `/webhooks`, `/dlq`, `/audit`, `/api-keys`, `/simulator`) are served directly via Thymeleaf server-side rendering or API controllers.
- No client-side HTML5 history routing mismatches occur, preventing `404 Not Found` page refreshes.

---

## 2. Environment Variables Configuration

| Variable | Default Value | Production Example / Description |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` | Active Spring profile (`prod`, `local`, `test`). |
| `SERVER_PORT` | `8080` | Port for the HTTP server (`8080` or `$PORT` on cloud platforms). |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/eventflow` | PostgreSQL connection string (`jdbc:postgresql://db.cloud.com:5432/eventflow_db`). |
| `SPRING_DATASOURCE_USERNAME` | `eventflow` | Database user. |
| `SPRING_DATASOURCE_PASSWORD` | `eventflow_pass` | Database password. |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Apache Kafka broker address (`kafka.cloud.com:9092` or Upstash/Aiven). |
| `SPRING_DATA_REDIS_HOST` | `localhost` | Redis server host. |
| `SPRING_DATA_REDIS_PORT` | `6379` | Redis server port. |
| `SPRING_DATA_REDIS_PASSWORD` | *(empty)* | Redis auth password if applicable. |
| `JWT_SECRET` | *(64-byte default)* | Production HMAC-SHA512 base64 or 64-char secret key. |
| `WEBHOOK_ENCRYPTION_KEY` | `ef_default_encryption_key_32bytes!!` | 32-character AES key for encrypting webhook secret keys at rest. |

---

## 3. Hassle-Free Cloud Deployment Options

### Option A: Complete Docker Compose Stack (Recommended)
Deploy everything (EventFlow App, PostgreSQL, Kafka, Redis, Zookeeper) in a single command on any VPS (AWS EC2, DigitalOcean Droplet, Hetzner, Linode, GCP Compute Engine).

```bash
# 1. Clone repository on server
git clone https://github.com/YugamNanda18/eventflow.git
cd eventflow

# 2. (Optional) Create .env file for custom production credentials
cat << 'EOF' > .env
SPRING_PROFILES_ACTIVE=prod
POSTGRES_USER=eventflow_prod
POSTGRES_PASSWORD=SuperSecretPassword123!
POSTGRES_DB=eventflow_prod
JWT_SECRET=c3VwZXJzZWNyZXRqd3RzZWNyZXRrZXlmb3JldmVudGZsb3dwcm9kdWN0aW9uZGVwbG95bWVudCE=
WEBHOOK_ENCRYPTION_KEY=prod_32byte_secret_key_12345678
EOF

# 3. Launch container stack in detached background mode
docker compose up -d --build

# 4. Verify status & logs
docker compose ps
docker compose logs -f eventflow-app
```

---

### Option B: Cloud PaaS Deployment (Render / Railway / Fly.io)

For cloud platform hosting, use managed databases and Kafka (e.g. Upstash Kafka / Redis + Managed Postgres):

1. **Database**: Provision a managed PostgreSQL instance (e.g., Neon, Render Postgres, Supabase). Flyway will automatically create all tables on first startup.
2. **Kafka & Redis**: Provision serverless Kafka & Redis via [Upstash](https://upstash.com/) or [Aiven](https://aiven.io/).
3. **App Container**: Connect your GitHub repository (`YugamNanda18/eventflow`) to Render/Railway using the included `Dockerfile`.
4. **Environment Variables**: Add `SPRING_DATASOURCE_URL`, `SPRING_KAFKA_BOOTSTRAP_SERVERS`, `SPRING_DATA_REDIS_HOST`, and `JWT_SECRET` in the platform web console.

---

## 4. Operational & Health Check Endpoints

- **Liveness Probe**: `http://<your-host>:8080/actuator/health/liveness` -> `{"status":"UP"}`
- **Readiness Probe**: `http://<your-host>:8080/actuator/health/readiness` -> `{"status":"UP"}`
- **Prometheus Metrics**: `http://<your-host>:8080/actuator/prometheus`
- **Swagger Documentation**: `http://<your-host>:8080/swagger-ui.html`

---

## 5. Troubleshooting & Maintenance

- **DB Migration Lock**: Flyway uses transactional schema migration. If a node shuts down mid-migration, Flyway handles rollback cleanly.
- **Kafka Topic Auto-Creation**: EventFlow creates topics dynamically (`eventflow.events`, `eventflow.webhooks`, etc.) if `auto.create.topics.enable=true` (enabled by default in `KafkaConfig.java`).
- **Log Inspection**: Inspect container logs using `docker compose logs -f eventflow-app`.
