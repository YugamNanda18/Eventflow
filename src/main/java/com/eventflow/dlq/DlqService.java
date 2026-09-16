package com.eventflow.dlq;

import com.eventflow.audit.AuditService;
import com.eventflow.common.context.TenantContext;
import com.eventflow.common.exception.ResourceNotFoundException;
import com.eventflow.common.exception.UnauthorizedTenantAccessException;
import com.eventflow.event.EventEntity;
import com.eventflow.kafka.KafkaProducerService;
import com.eventflow.outbox.OutboxEvent;
import com.eventflow.outbox.OutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class DlqService {

    private static final Logger log = LoggerFactory.getLogger(DlqService.class);

    private final DlqRepository dlqRepository;
    private final OutboxRepository outboxRepository;
    private final AuditService auditService;

    public DlqService(DlqRepository dlqRepository, OutboxRepository outboxRepository, AuditService auditService) {
        this.dlqRepository = dlqRepository;
        this.outboxRepository = outboxRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<DeadLetterEvent> getTenantDlqEvents(Pageable pageable) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new UnauthorizedTenantAccessException("Tenant context is required");
        }
        return dlqRepository.findByOrganizationId(tenantId, pageable);
    }

    @Transactional
    public void replayDlqEvent(String dlqId, String actorUserId) {
        String tenantId = TenantContext.getTenantId();
        DeadLetterEvent dlq = dlqRepository.findById(dlqId)
                .orElseThrow(() -> new ResourceNotFoundException("DeadLetterEvent", dlqId));

        if (!dlq.getOrganization().getId().equals(tenantId)) {
            throw new UnauthorizedTenantAccessException("Cannot access DLQ event from another tenant");
        }

        EventEntity event = dlq.getEvent();

        // Create new Outbox item to trigger re-processing without mutating original event
        OutboxEvent outbox = new OutboxEvent();
        outbox.setId("out_replay_" + UUID.randomUUID().toString().replace("-", ""));
        outbox.setEvent(event);
        outbox.setOrganization(dlq.getOrganization());
        outbox.setTopic("eventflow.events");
        outbox.setPartitionKey(tenantId + ":" + event.getSource());
        outbox.setPayload(event.getPayload());
        outbox.setStatus("PENDING");

        outboxRepository.save(outbox);

        dlq.setStatus("REPLAYED");
        dlq.setResolvedAt(Instant.now());
        dlq.setResolvedBy(actorUserId);
        dlqRepository.save(dlq);

        auditService.log(actorUserId, "USER", "DLQ_RETRIED", "DLQ", dlqId, Map.of("eventId", event.getId(), "status", "REPLAYED"));
        log.info("DLQ event {} replayed successfully by user {}", dlqId, actorUserId);
    }

    @Transactional
    public void discardDlqEvent(String dlqId, String actorUserId) {
        String tenantId = TenantContext.getTenantId();
        DeadLetterEvent dlq = dlqRepository.findById(dlqId)
                .orElseThrow(() -> new ResourceNotFoundException("DeadLetterEvent", dlqId));

        if (!dlq.getOrganization().getId().equals(tenantId)) {
            throw new UnauthorizedTenantAccessException("Cannot access DLQ event from another tenant");
        }

        dlq.setStatus("DISCARDED");
        dlq.setResolvedAt(Instant.now());
        dlq.setResolvedBy(actorUserId);
        dlqRepository.save(dlq);

        auditService.log(actorUserId, "USER", "DLQ_DISCARDED", "DLQ", dlqId, Map.of("eventId", dlq.getEvent().getId()));
        log.info("DLQ event {} discarded by user {}", dlqId, actorUserId);
    }
}
