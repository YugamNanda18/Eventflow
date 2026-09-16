package com.eventflow.webhook;

import com.eventflow.audit.AuditService;
import com.eventflow.common.context.TenantContext;
import com.eventflow.common.exception.ResourceNotFoundException;
import com.eventflow.common.exception.UnauthorizedTenantAccessException;
import com.eventflow.tenant.Organization;
import com.eventflow.tenant.OrganizationRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhookController {

    private final WebhookEndpointRepository endpointRepository;
    private final WebhookSubscriptionRepository subscriptionRepository;
    private final OrganizationRepository organizationRepository;
    private final AuditService auditService;
    private static final SecureRandom RANDOM = new SecureRandom();

    public WebhookController(WebhookEndpointRepository endpointRepository,
                             WebhookSubscriptionRepository subscriptionRepository,
                             OrganizationRepository organizationRepository,
                             AuditService auditService) {
        this.endpointRepository = endpointRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.organizationRepository = organizationRepository;
        this.auditService = auditService;
    }

    public static class CreateWebhookRequest {
        @NotBlank public String url;
        public String description;
        public List<String> eventTypes;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_DEVELOPER', 'SCOPE_webhooks:manage')")
    public ResponseEntity<WebhookEndpoint> createWebhook(@Valid @RequestBody CreateWebhookRequest request) {
        String tenantId = TenantContext.getTenantId();
        Organization org = organizationRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", tenantId));

        byte[] secretBytes = new byte[24];
        RANDOM.nextBytes(secretBytes);
        String secret = "whsec_" + HexFormat.of().formatHex(secretBytes);

        WebhookEndpoint endpoint = new WebhookEndpoint();
        endpoint.setId("wh_" + UUID.randomUUID().toString().replace("-", ""));
        endpoint.setOrganization(org);
        endpoint.setUrl(request.url);
        endpoint.setSecret(secret);
        endpoint.setDescription(request.description);
        endpoint.setActive(true);

        WebhookEndpoint saved = endpointRepository.save(endpoint);

        if (request.eventTypes != null) {
            for (String eventType : request.eventTypes) {
                WebhookSubscription sub = new WebhookSubscription(
                        "sub_" + UUID.randomUUID().toString().replace("-", ""),
                        saved,
                        eventType
                );
                subscriptionRepository.save(sub);
                saved.getSubscriptions().add(sub);
            }
        }

        auditService.log("system", "SYSTEM", "WEBHOOK_CREATED", "WEBHOOK", saved.getId(), Map.of("url", saved.getUrl()));
        return ResponseEntity.ok(saved);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OPERATOR', 'ROLE_DEVELOPER', 'ROLE_VIEWER')")
    public ResponseEntity<List<WebhookEndpoint>> getWebhooks() {
        String tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(endpointRepository.findByOrganizationId(tenantId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_DEVELOPER', 'SCOPE_webhooks:manage')")
    public ResponseEntity<Void> disableWebhook(@PathVariable String id) {
        String tenantId = TenantContext.getTenantId();
        WebhookEndpoint endpoint = endpointRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WebhookEndpoint", id));

        if (!endpoint.getOrganization().getId().equals(tenantId)) {
            throw new UnauthorizedTenantAccessException("Cannot access webhook from another tenant");
        }

        endpoint.setActive(false);
        endpointRepository.save(endpoint);

        auditService.log("system", "SYSTEM", "WEBHOOK_DISABLED", "WEBHOOK", id, Map.of("url", endpoint.getUrl()));
        return ResponseEntity.noContent().build();
    }
}
