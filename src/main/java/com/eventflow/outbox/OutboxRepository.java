package com.eventflow.outbox;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxEvent, String> {
    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(String status, Pageable pageable);

    @Query("SELECT COUNT(o) FROM OutboxEvent o WHERE o.status = 'PENDING'")
    long countPending();
}
