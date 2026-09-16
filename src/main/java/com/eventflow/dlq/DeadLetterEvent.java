package com.eventflow.dlq;

import com.eventflow.event.EventEntity;
import com.eventflow.tenant.Organization;
import com.eventflow.webhook.WebhookDelivery;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "dead_letter_events")
public class DeadLetterEvent {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private EventEntity event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_id")
    private WebhookDelivery delivery;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false)
    private String topic;

    @Column(name = "partition_num")
    private Integer partitionNum;

    @Column(name = "offset_num")
    private Long offsetNum;

    @Column(name = "failure_reason", nullable = false, columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "exception_type")
    private String exceptionType;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "first_failed_at", nullable = false)
    private Instant firstFailedAt;

    @Column(name = "last_failed_at", nullable = false)
    private Instant lastFailedAt;

    @Column(nullable = false)
    private String status = "UNRESOLVED"; // UNRESOLVED, REPLAYED, DISCARDED

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolved_by")
    private String resolvedBy;

    public DeadLetterEvent() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public EventEntity getEvent() { return event; }
    public void setEvent(EventEntity event) { this.event = event; }
    public WebhookDelivery getDelivery() { return delivery; }
    public void setDelivery(WebhookDelivery delivery) { this.delivery = delivery; }
    public Organization getOrganization() { return organization; }
    public void setOrganization(Organization organization) { this.organization = organization; }
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    public Integer getPartitionNum() { return partitionNum; }
    public void setPartitionNum(Integer partitionNum) { this.partitionNum = partitionNum; }
    public Long getOffsetNum() { return offsetNum; }
    public void setOffsetNum(Long offsetNum) { this.offsetNum = offsetNum; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public String getExceptionType() { return exceptionType; }
    public void setExceptionType(String exceptionType) { this.exceptionType = exceptionType; }
    public int getAttemptCount() { return attemptCount; }
    public void setAttemptCount(int attemptCount) { this.attemptCount = attemptCount; }
    public Instant getFirstFailedAt() { return firstFailedAt; }
    public void setFirstFailedAt(Instant firstFailedAt) { this.firstFailedAt = firstFailedAt; }
    public Instant getLastFailedAt() { return lastFailedAt; }
    public void setLastFailedAt(Instant lastFailedAt) { this.lastFailedAt = lastFailedAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }
    public String getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(String resolvedBy) { this.resolvedBy = resolvedBy; }
}
