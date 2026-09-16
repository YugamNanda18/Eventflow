package com.eventflow.event;

import com.eventflow.common.context.TenantContext;
import com.eventflow.common.exception.IdempotencyConflictException;
import com.eventflow.event.dto.EventIngestRequest;
import com.eventflow.event.dto.EventIngestResponse;
import com.eventflow.observability.MetricsService;
import com.eventflow.outbox.OutboxEvent;
import com.eventflow.outbox.OutboxRepository;
import com.eventflow.tenant.Organization;
import com.eventflow.tenant.OrganizationRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EventServiceTest {

    private EventRepository eventRepository;
    private OutboxRepository outboxRepository;
    private OrganizationRepository organizationRepository;
    private MetricsService metricsService;
    private EventService eventService;

    private Organization testOrg;

    @BeforeEach
    void setUp() {
        eventRepository = mock(EventRepository.class);
        outboxRepository = mock(OutboxRepository.class);
        organizationRepository = mock(OrganizationRepository.class);
        metricsService = new MetricsService(new SimpleMeterRegistry());

        eventService = new EventService(eventRepository, outboxRepository, organizationRepository, metricsService);

        testOrg = new Organization("org_democorp", "DemoCorp", "democorp");
        TenantContext.setTenantId(testOrg.getId());

        when(organizationRepository.findById("org_democorp")).thenReturn(Optional.of(testOrg));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void testIngestNewEvent_SavesEventAndOutbox() {
        EventIngestRequest req = new EventIngestRequest();
        req.setEventType("order.created");
        req.setSource("order-service");
        req.setIdempotencyKey("order-101-created");
        req.setPayload(Map.of("orderId", "ORD-101", "amount", 500));

        when(eventRepository.findByOrganizationIdAndIdempotencyKey(any(), any())).thenReturn(Optional.empty());

        EventIngestResponse response = eventService.ingestEvent(req);

        assertNotNull(response);
        assertNotNull(response.getEventId());
        assertEquals("RECEIVED", response.getStatus());
        assertFalse(response.isDuplicate());

        verify(eventRepository, times(1)).save(any(EventEntity.class));
        verify(outboxRepository, times(1)).save(any(OutboxEvent.class));
    }

    @Test
    void testIngestDuplicateEvent_ReturnsDuplicateFlag() {
        EventIngestRequest req = new EventIngestRequest();
        req.setEventType("order.created");
        req.setSource("order-service");
        req.setIdempotencyKey("order-101-created");
        req.setPayload(Map.of("orderId", "ORD-101", "amount", 500));

        EventEntity existing = new EventEntity();
        existing.setId("evt_existing");
        existing.setOrganization(testOrg);
        existing.setEventType("order.created");
        existing.setIdempotencyKey("order-101-created");
        existing.setPayload(com.eventflow.common.util.JsonUtils.toJson(req.getPayload()));
        existing.setStatus("RECEIVED");

        when(eventRepository.findByOrganizationIdAndIdempotencyKey("org_democorp", "order-101-created"))
                .thenReturn(Optional.of(existing));

        EventIngestResponse response = eventService.ingestEvent(req);

        assertTrue(response.isDuplicate());
        assertEquals("evt_existing", response.getEventId());
        verify(outboxRepository, never()).save(any());
    }

    @Test
    void testIngestSameIdempotencyKeyDifferentPayload_ThrowsConflict() {
        EventIngestRequest req = new EventIngestRequest();
        req.setEventType("order.created");
        req.setSource("order-service");
        req.setIdempotencyKey("order-101-created");
        req.setPayload(Map.of("orderId", "ORD-101", "amount", 99999));

        EventEntity existing = new EventEntity();
        existing.setId("evt_existing");
        existing.setOrganization(testOrg);
        existing.setEventType("order.created");
        existing.setIdempotencyKey("order-101-created");
        existing.setPayload("{\"amount\":500,\"orderId\":\"ORD-101\"}");

        when(eventRepository.findByOrganizationIdAndIdempotencyKey("org_democorp", "order-101-created"))
                .thenReturn(Optional.of(existing));

        assertThrows(IdempotencyConflictException.class, () -> eventService.ingestEvent(req));
    }
}
