package com.pearl.common.exception;

import java.util.Map;

public class AwsIntegrationException extends PearlException {

    public AwsIntegrationException(String message, boolean retryable, Throwable cause) {
        this(message, retryable, Map.of(), cause);
    }

    public AwsIntegrationException(
            String message,
            boolean retryable,
            Map<String, String> metadata,
            Throwable cause) {
        super(
                PearlErrorCode.AWS_INTEGRATION_FAILED.name(),
                message,
                retryable ? RetryClassification.RETRYABLE : RetryClassification.NON_RETRYABLE,
                metadata,
                cause);
    }
}
