package com.eventflow.event;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "event_schemas", uniqueConstraints = {
    @UniqueConstraint(name = "uq_event_type_version", columnNames = {"event_type_id", "version"})
})
public class EventSchema {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_type_id", nullable = false)
    private EventType eventType;

    @Column(nullable = false)
    private String version;

    @Column(name = "schema_json", nullable = false, columnDefinition = "TEXT")
    private String schemaJson;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public EventSchema() {}

    public EventSchema(String id, EventType eventType, String version, String schemaJson) {
        this.id = id;
        this.eventType = eventType;
        this.version = version;
        this.schemaJson = schemaJson;
        this.active = true;
        this.createdAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public EventType getEventType() { return eventType; }
    public void setEventType(EventType eventType) { this.eventType = eventType; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getSchemaJson() { return schemaJson; }
    public void setSchemaJson(String schemaJson) { this.schemaJson = schemaJson; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
