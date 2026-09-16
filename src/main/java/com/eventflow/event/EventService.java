package com.eventflow.event;

import com.eventflow.common.context.TenantContext;
import com.eventflow.common.exception.IdempotencyConflictException;
import com.eventflow.common.exception.ResourceNotFoundException;
import com.eventflow.common.exception.UnauthorizedTenantAccessException;
import com.eventflow.common.util.JsonUtils;
import com.eventflow.event.dto.EventDetailResponse;
import com.eventflow.event.dto.EventIngestRequest;
import com.eventflow.event.dto.EventIngestResponse;
import com.eventflow.observability.MetricsService;
import com.eventflow.outbox.OutboxEvent;
import com.eventflow.outbox.OutboxRepository;
import com.eventflow.tenant.Organization;
import com.eventflow.tenant.OrganizationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class EventService {

    private static final Logger log = LoggerFactory.getLogger(EventService.class);

    private final EventRepository eventRepository;
    private final OutboxRepository outboxRepository;
    private final OrganizationRepository organizationRepository;
    private final MetricsService metricsService;

    public EventService(EventRepository eventRepository,
                        OutboxRepository outboxRepository,
                        OrganizationRepository organizationRepository,
                        MetricsService metricsService) {
        this.eventRepository = eventRepository;
        this.outboxRepository = outboxRepository;
        this.organizationRepository = organizationRepository;
        this.metricsService = metricsService;
    }

    @Transactional
    public EventIngestResponse ingestEvent(EventIngestRequest request) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new UnauthorizedTenantAccessException("Tenant context is required for event ingestion");
        }

        Organization org = organizationRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", tenantId));

        String payloadJson = JsonUtils.toJson(request.getPayload());
        String metadataJson = request.getMetadata() != null ? JsonUtils.toJson(request.getMetadata()) : null;

        // Idempotency check
        Optional<EventEntity> existingOpt = eventRepository.findByOrganizationIdAndIdempotencyKey(tenantId, request.getIdempotencyKey());
        if (existingOpt.isPresent()) {
            EventEntity existing = existingOpt.get();
            if (existing.getPayload().equals(payloadJson)) {
                log.info("Idempotent duplicate event detected for idempotencyKey: {}", request.getIdempotencyKey());
                metricsService.incrementEventIngestion(tenantId, request.getEventType(), "duplicate");
                return new EventIngestResponse(
                        existing.getId(),
                        existing.getStatus(),
                        existing.getIdempotencyKey(),
                        existing.getCorrelationId(),
                        true,
                        existing.getCreatedAt()
                );
            } else {
                log.warn("Idempotency conflict for key {}: existing payload differs from new payload", request.getIdempotencyKey());
                throw new IdempotencyConflictException(request.getIdempotencyKey());
            }
        }

        String eventId = request.getEventId() != null && !request.getEventId().isBlank()
                ? request.getEventId()
                : "evt_" + UUID.randomUUID().toString().replace("-", "");

        String correlationId = request.getCorrelationId() != null && !request.getCorrelationId().isBlank()
                ? request.getCorrelationId()
                : (TenantContext.getCorrelationId() != null ? TenantContext.getCorrelationId() : "corr_" + UUID.randomUUID().toString().replace("-", ""));

        EventEntity event = new EventEntity();
        event.setId(eventId);
        event.setOrganization(org);
        event.setEventType(request.getEventType());
        event.setEventVersion(request.getEventVersion() != null ? request.getEventVersion() : "1.0");
        event.setSource(request.getSource());
        event.setIdempotencyKey(request.getIdempotencyKey());
        event.setCorrelationId(correlationId);
        event.setOccurredAt(request.getOccurredAt() != null ? request.getOccurredAt() : Instant.now());
        event.setPayload(payloadJson);
        event.setMetadata(metadataJson);
        event.setStatus("RECEIVED");

        eventRepository.save(event);

        // Transactional Outbox record creation
        OutboxEvent outbox = new OutboxEvent();
        outbox.setId("out_" + UUID.randomUUID().toString().replace("-", ""));
        outbox.setEvent(event);
        outbox.setOrganization(org);
        outbox.setTopic("eventflow.events");
        outbox.setPartitionKey(tenantId + ":" + request.getSource());
        outbox.setPayload(JsonUtils.toJson(eventToMap(event)));
        outbox.setStatus("PENDING");

        outboxRepository.save(outbox);

        metricsService.incrementEventIngestion(tenantId, request.getEventType(), "accepted");
        log.info("Event stored & outbox record created: eventId={}, tenantId={}", eventId, tenantId);

        return new EventIngestResponse(eventId, event.getStatus(), event.getIdempotencyKey(), correlationId, false, event.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public Page<EventDetailResponse> searchEvents(String eventType, String source, String status, String correlationId, Pageable pageable) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new UnauthorizedTenantAccessException("Tenant context is required");
        }

        return eventRepository.searchEvents(tenantId, eventType, source, status, correlationId, pageable)
                .map(this::mapToDetail);
    }

    @Transactional(readOnly = true)
    public EventDetailResponse getEventById(String id) {
        String tenantId = TenantContext.getTenantId();
        EventEntity event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event", id));

        if (tenantId != null && !event.getOrganization().getId().equals(tenantId)) {
            throw new UnauthorizedTenantAccessException("Cannot access event belonging to another organization");
        }

        return mapToDetail(event);
    }

    private EventDetailResponse mapToDetail(EventEntity entity) {
        EventDetailResponse dto = new EventDetailResponse();
        dto.setId(entity.getId());
        dto.setTenantId(entity.getOrganization().getId());
        dto.setEventType(entity.getEventType());
        dto.setEventVersion(entity.getEventVersion());
        dto.setSource(entity.getSource());
        dto.setIdempotencyKey(entity.getIdempotencyKey());
        dto.setCorrelationId(entity.getCorrelationId());
        dto.setOccurredAt(entity.getOccurredAt());
        dto.setPayload(entity.getPayload());
        dto.setMetadata(entity.getMetadata());
        dto.setStatus(entity.getStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    private java.util.Map<String, Object> eventToMap(EventEntity event) {
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("eventId", event.getId());
        map.put("eventType", event.getEventType());
        map.put("eventVersion", event.getEventVersion());
        map.put("tenantId", event.getOrganization().getId());
        map.put("source", event.getSource());
        map.put("idempotencyKey", event.getIdempotencyKey());
        map.put("correlationId", event.getCorrelationId());
        map.put("occurredAt", event.getOccurredAt().toString());
        map.put("payload", JsonUtils.fromJson(event.getPayload(), Object.class));
        if (event.getMetadata() != null) {
            map.put("metadata", JsonUtils.fromJson(event.getMetadata(), Object.class));
        }
        return map;
    }
}
