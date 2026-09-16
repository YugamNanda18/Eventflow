package com.eventflow.replay;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/replay")
public class ReplayController {

    private final EventReplayService replayService;

    public ReplayController(EventReplayService replayService) {
        this.replayService = replayService;
    }

    @PostMapping("/event/{eventId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OPERATOR')")
    public ResponseEntity<Map<String, String>> replayEvent(@PathVariable String eventId, Authentication authentication) {
        String actorUserId = authentication != null ? authentication.getName() : "operator";
        String outboxId = replayService.replayEvent(eventId, actorUserId);
        return ResponseEntity.ok(Map.of("message", "Event replay queued successfully", "outboxId", outboxId));
    }
}
