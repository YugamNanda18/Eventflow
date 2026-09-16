package com.eventflow.webhook;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, String> {
    Page<WebhookDelivery> findByOrganizationId(String organizationId, Pageable pageable);
    List<WebhookDelivery> findByStatusAndNextRetryAtBefore(String status, Instant now, Pageable pageable);
    long countByOrganizationIdAndStatus(String organizationId, String status);

    @Query("SELECT COUNT(d) FROM WebhookDelivery d WHERE d.organization.id = :orgId")
    long countTotalDeliveries(@Param("orgId") String orgId);
}
