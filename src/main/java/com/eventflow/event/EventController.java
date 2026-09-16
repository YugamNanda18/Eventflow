package com.eventflow.event;

import com.eventflow.event.dto.EventDetailResponse;
import com.eventflow.event.dto.EventIngestRequest;
import com.eventflow.event.dto.EventIngestResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_DEVELOPER', 'SCOPE_events:write')")
    public ResponseEntity<EventIngestResponse> ingestEvent(@Valid @RequestBody EventIngestRequest request) {
        return ResponseEntity.ok(eventService.ingestEvent(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OPERATOR', 'ROLE_DEVELOPER', 'ROLE_VIEWER', 'SCOPE_events:read')")
    public ResponseEntity<Page<EventDetailResponse>> getEvents(
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String correlationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(eventService.searchEvents(eventType, source, status, correlationId, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OPERATOR', 'ROLE_DEVELOPER', 'ROLE_VIEWER', 'SCOPE_events:read')")
    public ResponseEntity<EventDetailResponse> getEventById(@PathVariable String id) {
        return ResponseEntity.ok(eventService.getEventById(id));
    }
}
