# EventFlow REST API Specifications

Interactive Swagger OpenAPI UI available at: `http://localhost:8080/swagger-ui.html`

## Endpoints Summary

| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| POST | `/api/v1/auth/register` | User & Organization Registration | None |
| POST | `/api/v1/auth/login` | User Login (Returns JWT) | None |
| POST | `/api/v1/events` | Ingest Event | JWT / API Key (`events:write`) |
| GET | `/api/v1/events` | Search & Query Events | JWT / API Key (`events:read`) |
| GET | `/api/v1/events/{id}` | Get Event Detail | JWT / API Key (`events:read`) |
| POST | `/api/v1/webhooks` | Create Webhook Endpoint | JWT / API Key (`webhooks:manage`) |
| GET | `/api/v1/webhooks` | List Webhook Endpoints | JWT / API Key |
| DELETE | `/api/v1/webhooks/{id}` | Disable Webhook Endpoint | JWT / API Key |
| GET | `/api/v1/dlq` | List DLQ Events | Role: ADMIN, OPERATOR |
| POST | `/api/v1/dlq/{id}/replay` | Replay DLQ Event | Role: ADMIN, OPERATOR |
| POST | `/api/v1/dlq/{id}/discard` | Discard DLQ Event | Role: ADMIN, OPERATOR |
| POST | `/api/v1/replay/event/{id}` | Replay Event | Role: ADMIN, OPERATOR |
| POST | `/api/v1/api-keys` | Generate API Key | Role: ADMIN, DEVELOPER |
| GET | `/api/v1/api-keys` | List Tenant API Keys | Role: ADMIN, OPERATOR, DEVELOPER |
| DELETE | `/api/v1/api-keys/{id}` | Revoke API Key | Role: ADMIN |
| POST | `/api/v1/demo-webhook` | Built-in Test Receiver | None |
