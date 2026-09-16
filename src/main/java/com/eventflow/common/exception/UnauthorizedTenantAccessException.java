package com.eventflow.common.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedTenantAccessException extends ApiException {
    public UnauthorizedTenantAccessException(String message) {
        super(HttpStatus.FORBIDDEN, "TENANT_ACCESS_DENIED", message);
    }
}
