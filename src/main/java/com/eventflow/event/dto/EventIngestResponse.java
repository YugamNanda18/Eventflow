package com.eventflow.event.dto;

import java.time.Instant;

public class EventIngestResponse {
    private String eventId;
    private String status;
    private String idempotencyKey;
    private String correlationId;
    private boolean duplicate;
    private Instant receivedAt;

    public EventIngestResponse() {}

    public EventIngestResponse(String eventId, String status, String idempotencyKey, String correlationId, boolean duplicate, Instant receivedAt) {
        this.eventId = eventId;
        this.status = status;
        this.idempotencyKey = idempotencyKey;
        this.correlationId = correlationId;
        this.duplicate = duplicate;
        this.receivedAt = receivedAt;
    }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public boolean isDuplicate() { return duplicate; }
    public void setDuplicate(boolean duplicate) { this.duplicate = duplicate; }
    public Instant getReceivedAt() { return receivedAt; }
    public void setReceivedAt(Instant receivedAt) { this.receivedAt = receivedAt; }
}
