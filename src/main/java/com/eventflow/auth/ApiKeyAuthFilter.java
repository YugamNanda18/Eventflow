package com.eventflow.auth;

import com.eventflow.common.context.TenantContext;
import com.eventflow.common.util.HashUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private final ApiKeyRepository apiKeyRepository;

    public ApiKeyAuthFilter(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String apiKeyHeader = extractApiKey(request);
        if (apiKeyHeader != null && apiKeyHeader.startsWith("ef_live_")) {
            String keyHash = HashUtils.sha256(apiKeyHeader);
            Optional<ApiKey> apiKeyOpt = apiKeyRepository.findByKeyHash(keyHash);

            if (apiKeyOpt.isPresent()) {
                ApiKey apiKey = apiKeyOpt.get();
                if (!apiKey.isRevoked() && (apiKey.getExpiresAt() == null || apiKey.getExpiresAt().isAfter(Instant.now()))) {
                    String tenantId = apiKey.getOrganization().getId();
                    TenantContext.setTenantId(tenantId);

                    List<SimpleGrantedAuthority> authorities = Arrays.stream(apiKey.getScopes().split(","))
                            .map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope.trim()))
                            .toList();

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(apiKey, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractApiKey(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ef_live_")) {
            return authHeader.substring(7).trim();
        }
        String apiKeyParam = request.getHeader("X-API-Key");
        if (apiKeyParam != null && apiKeyParam.startsWith("ef_live_")) {
            return apiKeyParam.trim();
        }
        return null;
    }
}
