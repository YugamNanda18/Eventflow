package com.eventflow.outbox;

import com.eventflow.kafka.KafkaProducerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
public class OutboxPublisherScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisherScheduler.class);

    private final OutboxRepository outboxRepository;
    private final KafkaProducerService kafkaProducerService;

    public OutboxPublisherScheduler(OutboxRepository outboxRepository, KafkaProducerService kafkaProducerService) {
        this.outboxRepository = outboxRepository;
        this.kafkaProducerService = kafkaProducerService;
    }

    @Scheduled(fixedDelayString = "${eventflow.outbox.polling-rate-ms:2000}")
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pendingList = outboxRepository.findByStatusOrderByCreatedAtAsc("PENDING", PageRequest.of(0, 50));
        if (pendingList.isEmpty()) {
            return;
        }

        log.debug("Polling outbox: found {} pending events to publish", pendingList.size());

        for (OutboxEvent outbox : pendingList) {
            try {
                kafkaProducerService.sendMessage(outbox.getTopic(), outbox.getPartitionKey(), outbox.getPayload())
                        .whenComplete((result, ex) -> {
                            if (ex == null) {
                                outbox.setStatus("PUBLISHED");
                                outbox.setProcessedAt(Instant.now());
                                log.info("Successfully published outbox record {} to topic {}", outbox.getId(), outbox.getTopic());
                            } else {
                                int attempts = outbox.getAttemptCount() + 1;
                                outbox.setAttemptCount(attempts);
                                outbox.setLastError(ex.getMessage());
                                if (attempts >= 5) {
                                    outbox.setStatus("FAILED");
                                }
                                log.error("Failed to publish outbox record {}: {}", outbox.getId(), ex.getMessage());
                            }
                            outboxRepository.save(outbox);
                        });
            } catch (Exception e) {
                log.error("Error dispatching outbox record {}", outbox.getId(), e);
                outbox.setAttemptCount(outbox.getAttemptCount() + 1);
                outbox.setLastError(e.getMessage());
                outboxRepository.save(outbox);
            }
        }
    }
}
