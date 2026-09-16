package com.eventflow.replay;

import com.eventflow.audit.AuditService;
import com.eventflow.common.context.TenantContext;
import com.eventflow.common.exception.ResourceNotFoundException;
import com.eventflow.common.exception.UnauthorizedTenantAccessException;
import com.eventflow.event.EventEntity;
import com.eventflow.event.EventRepository;
import com.eventflow.outbox.OutboxEvent;
import com.eventflow.outbox.OutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class EventReplayService {

    private static final Logger log = LoggerFactory.getLogger(EventReplayService.class);

    private final EventRepository eventRepository;
    private final OutboxRepository outboxRepository;
    private final AuditService auditService;

    public EventReplayService(EventRepository eventRepository, OutboxRepository outboxRepository, AuditService auditService) {
        this.eventRepository = eventRepository;
        this.outboxRepository = outboxRepository;
        this.auditService = auditService;
    }

    @Transactional
    public String replayEvent(String eventId, String actorUserId) {
        String tenantId = TenantContext.getTenantId();
        EventEntity event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", eventId));

        if (!event.getOrganization().getId().equals(tenantId)) {
            throw new UnauthorizedTenantAccessException("Cannot replay event from another tenant");
        }

        auditService.log(actorUserId, "USER", "REPLAY_STARTED", "EVENT", eventId, Map.of("correlationId", event.getCorrelationId()));

        OutboxEvent outbox = new OutboxEvent();
        outbox.setId("out_replay_" + UUID.randomUUID().toString().replace("-", ""));
        outbox.setEvent(event);
        outbox.setOrganization(event.getOrganization());
        outbox.setTopic("eventflow.events");
        outbox.setPartitionKey(tenantId + ":" + event.getSource());
        outbox.setPayload(event.getPayload());
        outbox.setStatus("PENDING");

        outboxRepository.save(outbox);

        auditService.log(actorUserId, "USER", "REPLAY_COMPLETED", "EVENT", eventId, Map.of("outboxId", outbox.getId()));
        log.info("Event {} queued for replay by user {}", eventId, actorUserId);

        return outbox.getId();
    }
}
