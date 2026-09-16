package com.eventflow.auth;

import com.eventflow.auth.dto.ApiKeyResponse;
import com.eventflow.auth.dto.CreateApiKeyRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/api-keys")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    public ApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_DEVELOPER')")
    public ResponseEntity<ApiKeyResponse> createApiKey(@Valid @RequestBody CreateApiKeyRequest request, Authentication authentication) {
        String userId = authentication != null ? authentication.getName() : "system";
        return ResponseEntity.ok(apiKeyService.createApiKey(request, userId));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OPERATOR', 'ROLE_DEVELOPER', 'ROLE_VIEWER')")
    public ResponseEntity<List<ApiKeyResponse>> getApiKeys() {
        return ResponseEntity.ok(apiKeyService.getTenantApiKeys());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> revokeApiKey(@PathVariable String id) {
        apiKeyService.revokeApiKey(id);
        return ResponseEntity.noContent().build();
    }
}
