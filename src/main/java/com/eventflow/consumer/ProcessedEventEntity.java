package com.eventflow.consumer;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "processed_events", uniqueConstraints = {
    @UniqueConstraint(name = "uq_event_consumer", columnNames = {"event_id", "consumer_name"})
})
public class ProcessedEventEntity {

    @Id
    private String id;

    @Column(name = "event_id", nullable = false)
    private String eventId;

    @Column(name = "consumer_name", nullable = false)
    private String consumerName;

    @Column(name = "processed_at", nullable = false, updatable = false)
    private Instant processedAt = Instant.now();

    @Column(nullable = false)
    private String result;

    public ProcessedEventEntity() {}

    public ProcessedEventEntity(String id, String eventId, String consumerName, String result) {
        this.id = id;
        this.eventId = eventId;
        this.consumerName = consumerName;
        this.result = result;
        this.processedAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getConsumerName() { return consumerName; }
    public void setConsumerName(String consumerName) { this.consumerName = consumerName; }
    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
}
