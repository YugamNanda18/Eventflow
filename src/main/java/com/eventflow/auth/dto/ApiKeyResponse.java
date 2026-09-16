package com.eventflow.auth.dto;

import java.time.Instant;

public class ApiKeyResponse {
    private String id;
    private String name;
    private String keyPrefix;
    private String rawApiKey; // Only non-null upon initial creation!
    private String scopes;
    private boolean revoked;
    private Instant expiresAt;
    private Instant createdAt;

    public ApiKeyResponse() {}

    public ApiKeyResponse(String id, String name, String keyPrefix, String rawApiKey, String scopes, boolean revoked, Instant expiresAt, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.keyPrefix = keyPrefix;
        this.rawApiKey = rawApiKey;
        this.scopes = scopes;
        this.revoked = revoked;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getKeyPrefix() { return keyPrefix; }
    public void setKeyPrefix(String keyPrefix) { this.keyPrefix = keyPrefix; }
    public String getRawApiKey() { return rawApiKey; }
    public void setRawApiKey(String rawApiKey) { this.rawApiKey = rawApiKey; }
    public String getScopes() { return scopes; }
    public void setScopes(String scopes) { this.scopes = scopes; }
    public boolean isRevoked() { return revoked; }
    public void setRevoked(boolean revoked) { this.revoked = revoked; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
