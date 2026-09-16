package com.eventflow.consumer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEventEntity, String> {
    Optional<ProcessedEventEntity> findByEventIdAndConsumerName(String eventId, String consumerName);
    boolean existsByEventIdAndConsumerName(String eventId, String consumerName);
}
