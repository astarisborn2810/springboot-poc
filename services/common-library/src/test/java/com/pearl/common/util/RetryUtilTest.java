package com.pearl.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.pearl.common.exception.NonRetryableException;
import com.pearl.common.exception.RetryableException;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class RetryUtilTest {

    @Test
    void retriesRetryableFailures() {
        AtomicInteger attempts = new AtomicInteger();
        Callable<String> operation = () -> {
            if (attempts.incrementAndGet() == 1) {
                throw new RetryableException("try again");
            }
            return "ok";
        };

        String result = RetryUtil.execute(operation, 2, Duration.ZERO, null);

        assertEquals("ok", result);
        assertEquals(2, attempts.get());
    }

    @Test
    void doesNotRetryNonRetryableFailures() {
        AtomicInteger attempts = new AtomicInteger();
        Callable<String> operation = () -> {
            attempts.incrementAndGet();
            throw new NonRetryableException("stop");
        };

        assertThrows(NonRetryableException.class, () -> RetryUtil.execute(operation, 3, Duration.ZERO, null));
        assertEquals(1, attempts.get());
    }
}
