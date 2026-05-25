package com.pearl.common.exception;

import java.util.Map;

public class ProcessingException extends PearlException {

    public ProcessingException(String message, Throwable cause) {
        super(PearlErrorCode.PROCESSING_FAILED.name(), message, RetryClassification.NON_RETRYABLE, Map.of(), cause);
    }

    public ProcessingException(String message, Map<String, String> metadata, Throwable cause) {
        super(PearlErrorCode.PROCESSING_FAILED.name(), message, RetryClassification.NON_RETRYABLE, metadata, cause);
    }
}
