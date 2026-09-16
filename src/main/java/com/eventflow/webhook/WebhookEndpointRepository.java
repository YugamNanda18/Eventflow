package com.eventflow.webhook;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WebhookEndpointRepository extends JpaRepository<WebhookEndpoint, String> {
    List<WebhookEndpoint> findByOrganizationId(String organizationId);
    List<WebhookEndpoint> findByOrganizationIdAndActiveTrue(String organizationId);
}
