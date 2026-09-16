package com.eventflow.common.exception;

import org.springframework.http.HttpStatus;

public class IdempotencyConflictException extends ApiException {
    public IdempotencyConflictException(String idempotencyKey) {
        super(HttpStatus.CONFLICT, "IDEMPOTENCY_CONFLICT",
                "Idempotency key '" + idempotencyKey + "' was previously submitted with a different payload");
    }
}
