package com.pearl.common.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class PearlExceptionTest {

    @Test
    void classifiesRetryableException() {
        RetryableException exception = new RetryableException("retry", Map.of("service", "sqs"), null);

        assertTrue(exception.retryable());
        assertEquals(PearlErrorCode.RETRYABLE_FAILURE.name(), exception.errorCode());
        assertEquals("sqs", exception.metadata().get("service"));
    }

    @Test
    void classifiesValidationExceptionAsNonRetryable() {
        ValidationException exception = new ValidationException("bad payload");

        assertFalse(exception.retryable());
        assertEquals(PearlErrorCode.VALIDATION_FAILED.name(), exception.errorCode());
    }
}
