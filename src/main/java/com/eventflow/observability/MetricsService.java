package com.eventflow.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class MetricsService {

    private final MeterRegistry registry;

    public MetricsService(MeterRegistry registry) {
        this.registry = registry;
    }

    public void incrementEventIngestion(String tenantId, String eventType, String status) {
        Counter.builder("event_ingestion_total")
                .tag("tenantId", tenantId != null ? tenantId : "unknown")
                .tag("eventType", eventType != null ? eventType : "unknown")
                .tag("status", status)
                .register(registry)
                .increment();
    }

    public void incrementEventProcessing(String tenantId, String eventType, String status) {
        Counter.builder("event_processing_total")
                .tag("tenantId", tenantId != null ? tenantId : "unknown")
                .tag("eventType", eventType != null ? eventType : "unknown")
                .tag("status", status)
                .register(registry)
                .increment();
    }

    public void incrementWebhookDelivery(String tenantId, String endpointId, String status) {
        Counter.builder("webhook_delivery_total")
                .tag("tenantId", tenantId != null ? tenantId : "unknown")
                .tag("endpointId", endpointId != null ? endpointId : "unknown")
                .tag("status", status)
                .register(registry)
                .increment();
    }

    public void recordWebhookLatency(String endpointId, long millis) {
        Timer.builder("webhook_delivery_latency")
                .tag("endpointId", endpointId != null ? endpointId : "unknown")
                .register(registry)
                .record(millis, TimeUnit.MILLISECONDS);
    }

    public void incrementDlq(String tenantId, String reason) {
        Counter.builder("dlq_total")
                .tag("tenantId", tenantId != null ? tenantId : "unknown")
                .tag("reason", reason != null ? reason : "unknown")
                .register(registry)
                .increment();
    }
}
