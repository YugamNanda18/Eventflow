package com.eventflow.webhook;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "webhook_subscriptions", uniqueConstraints = {
    @UniqueConstraint(name = "uq_endpoint_event_type", columnNames = {"webhook_endpoint_id", "event_type"})
})
public class WebhookSubscription {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "webhook_endpoint_id", nullable = false)
    private WebhookEndpoint webhookEndpoint;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public WebhookSubscription() {}

    public WebhookSubscription(String id, WebhookEndpoint webhookEndpoint, String eventType) {
        this.id = id;
        this.webhookEndpoint = webhookEndpoint;
        this.eventType = eventType;
        this.createdAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public WebhookEndpoint getWebhookEndpoint() { return webhookEndpoint; }
    public void setWebhookEndpoint(WebhookEndpoint webhookEndpoint) { this.webhookEndpoint = webhookEndpoint; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
