package com.eventflow.event;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<EventEntity, String> {
    Optional<EventEntity> findByOrganizationIdAndIdempotencyKey(String organizationId, String idempotencyKey);
    Page<EventEntity> findByOrganizationId(String organizationId, Pageable pageable);

    @Query("SELECT e FROM EventEntity e WHERE e.organization.id = :orgId " +
           "AND (:eventType IS NULL OR e.eventType = :eventType) " +
           "AND (:source IS NULL OR e.source = :source) " +
           "AND (:status IS NULL OR e.status = :status) " +
           "AND (:correlationId IS NULL OR e.correlationId = :correlationId)")
    Page<EventEntity> searchEvents(
            @Param("orgId") String orgId,
            @Param("eventType") String eventType,
            @Param("source") String source,
            @Param("status") String status,
            @Param("correlationId") String correlationId,
            Pageable pageable
    );
}
