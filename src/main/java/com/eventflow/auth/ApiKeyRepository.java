package com.eventflow.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, String> {
    Optional<ApiKey> findByKeyHash(String keyHash);
    List<ApiKey> findByKeyPrefix(String keyPrefix);
    List<ApiKey> findByOrganizationId(String organizationId);
}
