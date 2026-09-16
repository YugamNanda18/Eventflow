package com.eventflow.simulator;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/simulator")
public class FailureSimulatorController {

    private final FailureSimulatorService simulatorService;

    public FailureSimulatorController(FailureSimulatorService simulatorService) {
        this.simulatorService = simulatorService;
    }

    @GetMapping("/status")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OPERATOR', 'ROLE_DEVELOPER')")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(Map.of(
                "simulateWebhook500", simulatorService.isSimulateWebhook500(),
                "simulateWebhookTimeout", simulatorService.isSimulateWebhookTimeout(),
                "simulateConsumerFailure", simulatorService.isSimulateConsumerFailure()
        ));
    }

    @PostMapping("/configure")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OPERATOR', 'ROLE_DEVELOPER')")
    public ResponseEntity<Map<String, String>> configure(
            @RequestParam(required = false) Boolean webhook500,
            @RequestParam(required = false) Boolean webhookTimeout,
            @RequestParam(required = false) Boolean consumerFailure) {

        if (webhook500 != null) simulatorService.setSimulateWebhook500(webhook500);
        if (webhookTimeout != null) simulatorService.setSimulateWebhookTimeout(webhookTimeout);
        if (consumerFailure != null) simulatorService.setSimulateConsumerFailure(consumerFailure);

        return ResponseEntity.ok(Map.of("message", "Failure simulation settings updated"));
    }

    @PostMapping("/reset")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OPERATOR', 'ROLE_DEVELOPER')")
    public ResponseEntity<Map<String, String>> reset() {
        simulatorService.reset();
        return ResponseEntity.ok(Map.of("message", "Failure simulator reset to normal operation"));
    }
}
