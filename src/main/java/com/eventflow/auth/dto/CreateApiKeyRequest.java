package com.eventflow.auth.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

public class CreateApiKeyRequest {
    @NotBlank
    private String name;
    @NotBlank
    private String scopes; // events:write,events:read,webhooks:manage
    private Instant expiresAt;

    public CreateApiKeyRequest() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getScopes() { return scopes; }
    public void setScopes(String scopes) { this.scopes = scopes; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
}
