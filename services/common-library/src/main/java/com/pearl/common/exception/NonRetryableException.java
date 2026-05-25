package com.pearl.common.exception;

import java.util.Map;

public class NonRetryableException extends PearlException {

    public NonRetryableException(String message) {
        this(message, Map.of(), null);
    }

    public NonRetryableException(String message, Throwable cause) {
        this(message, Map.of(), cause);
    }

    public NonRetryableException(String message, Map<String, String> metadata, Throwable cause) {
        super(PearlErrorCode.NON_RETRYABLE_FAILURE.name(), message, RetryClassification.NON_RETRYABLE, metadata, cause);
    }
}
