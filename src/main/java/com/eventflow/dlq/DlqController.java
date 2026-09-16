package com.eventflow.dlq;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dlq")
public class DlqController {

    private final DlqService dlqService;

    public DlqController(DlqService dlqService) {
        this.dlqService = dlqService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OPERATOR')")
    public ResponseEntity<Page<DeadLetterEvent>> getDlq(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(dlqService.getTenantDlqEvents(pageable));
    }

    @PostMapping("/{id}/replay")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OPERATOR')")
    public ResponseEntity<Void> replayDlq(@PathVariable String id, Authentication authentication) {
        String userId = authentication != null ? authentication.getName() : "operator";
        dlqService.replayDlqEvent(id, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/discard")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OPERATOR')")
    public ResponseEntity<Void> discardDlq(@PathVariable String id, Authentication authentication) {
        String userId = authentication != null ? authentication.getName() : "operator";
        dlqService.discardDlqEvent(id, userId);
        return ResponseEntity.ok().build();
    }
}
