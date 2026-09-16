package com.eventflow.webhook;

import com.eventflow.common.util.HmacUtils;
import com.eventflow.common.util.JsonUtils;
import com.eventflow.dlq.DeadLetterEvent;
import com.eventflow.dlq.DlqRepository;
import com.eventflow.event.EventEntity;
import com.eventflow.observability.MetricsService;
import com.eventflow.simulator.FailureSimulatorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class WebhookEngineService {

    private static final Logger log = LoggerFactory.getLogger(WebhookEngineService.class);

    private final WebhookDeliveryRepository deliveryRepository;
    private final DeliveryAttemptRepository attemptRepository;
    private final DlqRepository dlqRepository;
    private final MetricsService metricsService;
    private final FailureSimulatorService failureSimulatorService;
    private final RestTemplate restTemplate;

    @Value("${eventflow.webhook.signature-header:X-EventFlow-Signature}")
    private String signatureHeaderName;

    public WebhookEngineService(WebhookDeliveryRepository deliveryRepository,
                                DeliveryAttemptRepository attemptRepository,
                                DlqRepository dlqRepository,
                                MetricsService metricsService,
                                FailureSimulatorService failureSimulatorService) {
        this.deliveryRepository = deliveryRepository;
        this.attemptRepository = attemptRepository;
        this.dlqRepository = dlqRepository;
        this.metricsService = metricsService;
        this.failureSimulatorService = failureSimulatorService;
        this.restTemplate = new RestTemplate();
    }

    @Transactional
    public void executeDelivery(WebhookDelivery delivery) {
        WebhookEndpoint endpoint = delivery.getWebhookEndpoint();
        EventEntity event = delivery.getEvent();
        int attemptNum = delivery.getAttemptCount() + 1;

        Instant startedAt = Instant.now();
        String payloadJson = buildWebhookPayload(event);
        long timestamp = Instant.now().getEpochSecond();
        String signature = HmacUtils.computeHmacSha256(timestamp + "." + payloadJson, endpoint.getSecret());
        String signatureHeaderValue = "t=" + timestamp + ",v1=" + signature;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(signatureHeaderName, signatureHeaderValue);
        headers.set("X-Correlation-ID", event.getCorrelationId());
        headers.set("User-Agent", "EventFlow-Webhook-Engine/1.0");

        HttpEntity<String> httpEntity = new HttpEntity<>(payloadJson, headers);

        DeliveryAttempt attempt = new DeliveryAttempt();
        attempt.setId("att_" + UUID.randomUUID().toString().replace("-", ""));
        attempt.setDelivery(delivery);
        attempt.setAttemptNumber(attemptNum);
        attempt.setStartedAt(startedAt);

        // Check Dev Failure Simulation
        if (failureSimulatorService.isSimulateWebhook500()) {
            recordFailure(delivery, attempt, 500, "Simulated HTTP 500 Internal Server Error", startedAt);
            return;
        }
        if (failureSimulatorService.isSimulateWebhookTimeout()) {
            recordFailure(delivery, attempt, 504, "Simulated HTTP Webhook Gateway Timeout", startedAt);
            return;
        }

        try {
            ResponseEntity<String> response = restTemplate.exchange(endpoint.getUrl(), HttpMethod.POST, httpEntity, String.class);
            long latencyMs = Instant.now().toEpochMilli() - startedAt.toEpochMilli();

            attempt.setCompletedAt(Instant.now());
            attempt.setHttpStatus(response.getStatusCode().value());
            attempt.setResponseTimeMs(latencyMs);

            if (response.getStatusCode().is2xxSuccessful()) {
                attempt.setStatus("SUCCESS");
                delivery.setStatus("SUCCESS");
                delivery.setAttemptCount(attemptNum);
                delivery.setNextRetryAt(null);
                deliveryRepository.save(delivery);
                attemptRepository.save(attempt);

                metricsService.incrementWebhookDelivery(delivery.getOrganization().getId(), endpoint.getId(), "success");
                metricsService.recordWebhookLatency(endpoint.getId(), latencyMs);
                log.info("Webhook delivery {} succeeded on attempt {} (HTTP {}, {}ms)", delivery.getId(), attemptNum, response.getStatusCode().value(), latencyMs);
            } else {
                recordFailure(delivery, attempt, response.getStatusCode().value(), "HTTP status: " + response.getStatusCode().value(), startedAt);
            }
        } catch (Exception e) {
            log.warn("Webhook delivery {} failed on attempt {}: {}", delivery.getId(), attemptNum, e.getMessage());
            recordFailure(delivery, attempt, 500, e.getMessage(), startedAt);
        }
    }

    private void recordFailure(WebhookDelivery delivery, DeliveryAttempt attempt, int httpStatus, String errorMsg, Instant startedAt) {
        long latencyMs = Instant.now().toEpochMilli() - startedAt.toEpochMilli();
        int attemptNum = delivery.getAttemptCount() + 1;

        attempt.setCompletedAt(Instant.now());
        attempt.setStatus("FAILURE");
        attempt.setHttpStatus(httpStatus);
        attempt.setResponseTimeMs(latencyMs);
        attempt.setErrorMessage(errorMsg);

        delivery.setAttemptCount(attemptNum);

        if (attemptNum >= delivery.getMaxRetries()) {
            delivery.setStatus("FAILED");
            delivery.setNextRetryAt(null);
            deliveryRepository.save(delivery);

            // Move to Dead Letter Queue
            DeadLetterEvent dlq = new DeadLetterEvent();
            dlq.setId("dlq_" + UUID.randomUUID().toString().replace("-", ""));
            dlq.setEvent(delivery.getEvent());
            dlq.setDelivery(delivery);
            dlq.setOrganization(delivery.getOrganization());
            dlq.setTopic("eventflow.webhooks");
            dlq.setFailureReason("Webhook delivery failed after " + attemptNum + " attempts: " + errorMsg);
            dlq.setExceptionType("WebhookDeliveryException");
            dlq.setAttemptCount(attemptNum);
            dlq.setFirstFailedAt(delivery.getCreatedAt());
            dlq.setLastFailedAt(Instant.now());
            dlq.setStatus("UNRESOLVED");

            dlqRepository.save(dlq);
            metricsService.incrementDlq(delivery.getOrganization().getId(), "webhook_max_retries_exceeded");
            log.error("Webhook delivery {} permanently failed after {} attempts, routed to DLQ {}", delivery.getId(), attemptNum, dlq.getId());
        } else {
            // Exponential Backoff with Jitter: 1s, 5s, 30s, 2m, 10m
            long backoffSeconds = getExponentialBackoffSeconds(attemptNum);
            Instant nextRetryAt = Instant.now().plusSeconds(backoffSeconds);

            delivery.setStatus("RETRYING");
            delivery.setNextRetryAt(nextRetryAt);
            attempt.setNextRetryAt(nextRetryAt);

            deliveryRepository.save(delivery);
            log.info("Webhook delivery {} scheduled for retry #{} at {}", delivery.getId(), attemptNum + 1, nextRetryAt);
        }

        attemptRepository.save(attempt);
        metricsService.incrementWebhookDelivery(delivery.getOrganization().getId(), delivery.getWebhookEndpoint().getId(), "failure");
    }

    private long getExponentialBackoffSeconds(int attemptNum) {
        return switch (attemptNum) {
            case 1 -> 1;
            case 2 -> 5;
            case 3 -> 30;
            case 4 -> 120;
            default -> 600;
        };
    }

    private String buildWebhookPayload(EventEntity event) {
        Map<String, Object> map = new HashMap<>();
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
        return JsonUtils.toJson(map);
    }
}
