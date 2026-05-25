package com.pearl.common.exception;

import java.util.Map;

public class PearlException extends RuntimeException {

    private final String errorCode;
    private final RetryClassification retryClassification;
    private final Map<String, String> metadata;

    public PearlException(String errorCode, String message, RetryClassification retryClassification) {
        this(errorCode, message, retryClassification, Map.of(), null);
    }

    public PearlException(
            String errorCode,
            String message,
            RetryClassification retryClassification,
            Map<String, String> metadata) {
        this(errorCode, message, retryClassification, metadata, null);
    }

    public PearlException(
            String errorCode,
            String message,
            RetryClassification retryClassification,
            Map<String, String> metadata,
            Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode == null ? PearlErrorCode.UNKNOWN_FAILURE.name() : errorCode;
        this.retryClassification = retryClassification == null
                ? RetryClassification.NON_RETRYABLE
                : retryClassification;
        this.metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public String errorCode() {
        return errorCode;
    }

    public RetryClassification retryClassification() {
        return retryClassification;
    }

    public boolean retryable() {
        return retryClassification == RetryClassification.RETRYABLE;
    }

    public Map<String, String> metadata() {
        return metadata;
    }
}
