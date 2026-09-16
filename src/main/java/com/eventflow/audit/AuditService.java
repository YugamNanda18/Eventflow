package com.eventflow.audit;

import com.eventflow.common.context.TenantContext;
import com.eventflow.common.util.JsonUtils;
import com.eventflow.tenant.Organization;
import com.eventflow.tenant.OrganizationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class AuditService {

    private final AuditRepository auditRepository;
    private final OrganizationRepository organizationRepository;

    public AuditService(AuditRepository auditRepository, OrganizationRepository organizationRepository) {
        this.auditRepository = auditRepository;
        this.organizationRepository = organizationRepository;
    }

    @Transactional
    public void log(String actorId, String actorType, String action, String resourceType, String resourceId, Map<String, Object> metadata) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) return;

        Organization org = organizationRepository.findById(tenantId).orElse(null);
        if (org == null) return;

        AuditLog log = new AuditLog();
        log.setId("audit_" + UUID.randomUUID().toString().replace("-", ""));
        log.setOrganization(org);
        log.setActorId(actorId != null ? actorId : "system");
        log.setActorType(actorType != null ? actorType : "SYSTEM");
        log.setAction(action);
        log.setResourceType(resourceType);
        log.setResourceId(resourceId);
        log.setCorrelationId(TenantContext.getCorrelationId());
        if (metadata != null) {
            log.setMetadataJson(JsonUtils.toJson(metadata));
        }

        auditRepository.save(log);
    }
}
