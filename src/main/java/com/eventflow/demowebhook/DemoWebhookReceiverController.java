package com.eventflow.demowebhook;

import com.eventflow.common.util.HmacUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/demo-webhook")
public class DemoWebhookReceiverController {

    private static final Logger log = LoggerFactory.getLogger(DemoWebhookReceiverController.class);

    @PostMapping
    public ResponseEntity<Map<String, Object>> receiveDemoWebhook(
            @RequestHeader(value = "X-EventFlow-Signature", required = false) String signatureHeader,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId,
            @RequestParam(value = "mode", defaultValue = "200") String mode,
            @RequestBody String rawBody) {

        log.info("Demo Webhook Receiver received request. Mode={}, CorrelationId={}, Signature={}", mode, correlationId, signatureHeader);
        log.info("Demo Webhook Raw Body: {}", rawBody);

        if ("500".equalsIgnoreCase(mode)) {
            log.warn("Demo Webhook Receiver returning forced 500 error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", "Forced HTTP 500 from demo receiver"));
        }

        if ("timeout".equalsIgnoreCase(mode)) {
            try {
                log.warn("Demo Webhook Receiver simulating timeout (sleeping 6 seconds)...");
                Thread.sleep(6000);
            } catch (InterruptedException ignored) {}
        }

        boolean validSignature = false;
        if (signatureHeader != null && signatureHeader.contains("v1=")) {
            String[] parts = signatureHeader.split(",");
            String timestamp = parts[0].replace("t=", "");
            String signature = parts[1].replace("v1=", "");
            // In demo receiver, signature verification check log
            log.info("Signature timestamp: {}, hex: {}", timestamp, signature);
            validSignature = true;
        }

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Demo webhook delivered successfully",
                "signatureValid", validSignature,
                "correlationId", correlationId != null ? correlationId : "none"
        ));
    }
}
