package com.eventflow.retry;

import com.eventflow.webhook.WebhookDelivery;
import com.eventflow.webhook.WebhookDeliveryRepository;
import com.eventflow.webhook.WebhookEngineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class WebhookRetryScheduler {

    private static final Logger log = LoggerFactory.getLogger(WebhookRetryScheduler.class);

    private final WebhookDeliveryRepository deliveryRepository;
    private final WebhookEngineService webhookEngineService;

    public WebhookRetryScheduler(WebhookDeliveryRepository deliveryRepository, WebhookEngineService webhookEngineService) {
        this.deliveryRepository = deliveryRepository;
        this.webhookEngineService = webhookEngineService;
    }

    @Scheduled(fixedDelay = 5000)
    public void processScheduledRetries() {
        List<WebhookDelivery> dueDeliveries = deliveryRepository.findByStatusAndNextRetryAtBefore("RETRYING", Instant.now(), PageRequest.of(0, 50));
        if (dueDeliveries.isEmpty()) {
            return;
        }

        log.info("Webhook retry scheduler: executing {} due retries", dueDeliveries.size());
        for (WebhookDelivery delivery : dueDeliveries) {
            try {
                webhookEngineService.executeDelivery(delivery);
            } catch (Exception e) {
                log.error("Error executing retry for delivery {}: {}", delivery.getId(), e.getMessage());
            }
        }
    }
}
