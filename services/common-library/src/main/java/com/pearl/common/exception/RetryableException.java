package com.pearl.common.exception;

import java.util.Map;

public class RetryableException extends PearlException {

    public RetryableException(String message) {
        this(message, Map.of(), null);
    }

    public RetryableException(String message, Throwable cause) {
        this(message, Map.of(), cause);
    }

    public RetryableException(String message, Map<String, String> metadata, Throwable cause) {
        super(PearlErrorCode.RETRYABLE_FAILURE.name(), message, RetryClassification.RETRYABLE, metadata, cause);
    }
}
