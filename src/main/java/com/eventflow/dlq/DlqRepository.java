package com.eventflow.dlq;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DlqRepository extends JpaRepository<DeadLetterEvent, String> {
    Page<DeadLetterEvent> findByOrganizationId(String organizationId, Pageable pageable);
    long countByOrganizationIdAndStatus(String organizationId, String status);
}
