package com.eventflow.auth;

import com.eventflow.auth.dto.ApiKeyResponse;
import com.eventflow.auth.dto.CreateApiKeyRequest;
import com.eventflow.common.context.TenantContext;
import com.eventflow.common.exception.ApiException;
import com.eventflow.common.exception.ResourceNotFoundException;
import com.eventflow.common.exception.UnauthorizedTenantAccessException;
import com.eventflow.common.util.HashUtils;
import com.eventflow.tenant.Organization;
import com.eventflow.tenant.OrganizationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final OrganizationRepository organizationRepository;
    private static final SecureRandom RANDOM = new SecureRandom();

    public ApiKeyService(ApiKeyRepository apiKeyRepository, OrganizationRepository organizationRepository) {
        this.apiKeyRepository = apiKeyRepository;
        this.organizationRepository = organizationRepository;
    }

    @Transactional
    public ApiKeyResponse createApiKey(CreateApiKeyRequest request, String createdByUserId) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new UnauthorizedTenantAccessException("Tenant context is required to create API keys");
        }

        Organization org = organizationRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", tenantId));

        byte[] randomBytes = new byte[16];
        RANDOM.nextBytes(randomBytes);
        String randomHex = HexFormat.of().formatHex(randomBytes);
        String rawApiKey = "ef_live_" + randomHex;
        String prefix = rawApiKey.substring(0, 12);
        String keyHash = HashUtils.sha256(rawApiKey);

        ApiKey apiKey = new ApiKey();
        apiKey.setId("key_" + UUID.randomUUID().toString().replace("-", ""));
        apiKey.setOrganization(org);
        apiKey.setName(request.getName());
        apiKey.setKeyPrefix(prefix);
        apiKey.setKeyHash(keyHash);
        apiKey.setScopes(request.getScopes());
        apiKey.setExpiresAt(request.getExpiresAt());
        apiKey.setCreatedBy(createdByUserId);

        apiKeyRepository.save(apiKey);

        return new ApiKeyResponse(
                apiKey.getId(),
                apiKey.getName(),
                apiKey.getKeyPrefix(),
                rawApiKey, // Raw key returned ONCE
                apiKey.getScopes(),
                apiKey.isRevoked(),
                apiKey.getExpiresAt(),
                apiKey.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<ApiKeyResponse> getTenantApiKeys() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new UnauthorizedTenantAccessException("Tenant context is required");
        }

        return apiKeyRepository.findByOrganizationId(tenantId).stream()
                .map(key -> new ApiKeyResponse(
                        key.getId(),
                        key.getName(),
                        key.getKeyPrefix(),
                        null, // Never show raw key after initial creation
                        key.getScopes(),
                        key.isRevoked(),
                        key.getExpiresAt(),
                        key.getCreatedAt()
                ))
                .toList();
    }

    @Transactional
    public void revokeApiKey(String keyId) {
        String tenantId = TenantContext.getTenantId();
        ApiKey apiKey = apiKeyRepository.findById(keyId)
                .orElseThrow(() -> new ResourceNotFoundException("ApiKey", keyId));

        if (!apiKey.getOrganization().getId().equals(tenantId)) {
            throw new UnauthorizedTenantAccessException("Cannot modify API Key belonging to another tenant");
        }

        apiKey.setRevoked(true);
        apiKeyRepository.save(apiKey);
    }
}
