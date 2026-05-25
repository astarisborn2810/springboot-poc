package com.pearl.common.exception;

import java.util.Map;

public class ValidationException extends PearlException {

    public ValidationException(String message) {
        this(message, Map.of());
    }

    public ValidationException(String message, Map<String, String> metadata) {
        super(PearlErrorCode.VALIDATION_FAILED.name(), message, RetryClassification.NON_RETRYABLE, metadata);
    }
}
