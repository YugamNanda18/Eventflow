package com.eventflow.consumer;

import com.eventflow.common.util.JsonUtils;
import com.eventflow.event.EventEntity;
import com.eventflow.event.EventRepository;
import com.eventflow.observability.MetricsService;
import com.eventflow.webhook.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class EventConsumerService {

    private static final Logger log = LoggerFactory.getLogger(EventConsumerService.class);
    private static final String CONSUMER_NAME = "WebhookConsumerGroup";

    private final ProcessedEventRepository processedEventRepository;
    private final EventRepository eventRepository;
    private final WebhookSubscriptionRepository subscriptionRepository;
    private final WebhookDeliveryRepository deliveryRepository;
    private final WebhookEngineService webhookEngineService;
    private final MetricsService metricsService;

    public EventConsumerService(ProcessedEventRepository processedEventRepository,
                                EventRepository eventRepository,
                                WebhookSubscriptionRepository subscriptionRepository,
                                WebhookDeliveryRepository deliveryRepository,
                                WebhookEngineService webhookEngineService,
                                MetricsService metricsService) {
        this.processedEventRepository = processedEventRepository;
        this.eventRepository = eventRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.deliveryRepository = deliveryRepository;
        this.webhookEngineService = webhookEngineService;
        this.metricsService = metricsService;
    }

    @KafkaListener(topics = "eventflow.events", groupId = "${spring.kafka.consumer.group-id:eventflow-consumer-group}")
    @Transactional
    public void consumeEvent(String message) {
        try {
            JsonNode root = JsonUtils.getMapper().readTree(message);
            String eventId = root.get("eventId").asText();
            String eventType = root.get("eventType").asText();
            String tenantId = root.get("tenantId").asText();

            log.info("Kafka consumer received event: eventId={}, eventType={}, tenantId={}", eventId, eventType, tenantId);

            // Consumer Idempotency Check
            if (processedEventRepository.existsByEventIdAndConsumerName(eventId, CONSUMER_NAME)) {
                log.info("Consumer idempotency check: eventId {} already processed by {}, skipping", eventId, CONSUMER_NAME);
                return;
            }

            ProcessedEventEntity processed = new ProcessedEventEntity(
                    "proc_" + UUID.randomUUID().toString().replace("-", ""),
                    eventId,
                    CONSUMER_NAME,
                    "SUCCESS"
            );
            processedEventRepository.save(processed);

            EventEntity event = eventRepository.findById(eventId).orElse(null);
            if (event == null) {
                log.warn("Event entity not found for ID: {}", eventId);
                return;
            }

            // Find Webhook Subscriptions matching eventType
            List<WebhookSubscription> subscriptions = subscriptionRepository.findByEventType(eventType);
            for (WebhookSubscription sub : subscriptions) {
                WebhookEndpoint endpoint = sub.getWebhookEndpoint();
                if (endpoint.isActive() && endpoint.getOrganization().getId().equals(tenantId)) {
                    WebhookDelivery delivery = new WebhookDelivery();
                    delivery.setId("del_" + UUID.randomUUID().toString().replace("-", ""));
                    delivery.setWebhookEndpoint(endpoint);
                    delivery.setEvent(event);
                    delivery.setOrganization(event.getOrganization());
                    delivery.setStatus("PENDING");
                    delivery.setAttemptCount(0);
                    delivery.setMaxRetries(5);

                    deliveryRepository.save(delivery);
                    log.info("Created Webhook Delivery {} for endpoint {}", delivery.getId(), endpoint.getUrl());

                    // Execute delivery
                    webhookEngineService.executeDelivery(delivery);
                }
            }

            metricsService.incrementEventProcessing(tenantId, eventType, "success");

        } catch (Exception e) {
            log.error("Error consuming Kafka event message: {}", message, e);
        }
    }
}
