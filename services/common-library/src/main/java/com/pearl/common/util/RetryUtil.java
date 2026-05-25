package com.pearl.common.util;

import com.pearl.common.exception.RetryableException;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Predicate;

public final class RetryUtil {

    private RetryUtil() {
    }

    public static <T> T execute(
            Callable<T> operation,
            int maxAttempts,
            Duration initialBackoff,
            Predicate<Throwable> retryPredicate) {
        Objects.requireNonNull(operation, "operation must not be null");
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be at least 1");
        }
        Duration backoff = initialBackoff == null ? Duration.ZERO : initialBackoff;
        Predicate<Throwable> predicate = retryPredicate == null ? RetryUtil::defaultRetryPredicate : retryPredicate;

        Throwable lastFailure = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return operation.call();
            } catch (Throwable ex) {
                lastFailure = ex;
                if (attempt == maxAttempts || !predicate.test(ex)) {
                    throw propagate(ex);
                }
                sleep(backoff.multipliedBy(attempt));
            }
        }
        throw propagate(lastFailure);
    }

    public static void executeRunnable(
            Runnable operation,
            int maxAttempts,
            Duration initialBackoff,
            Predicate<Throwable> retryPredicate) {
        execute(() -> {
            operation.run();
            return null;
        }, maxAttempts, initialBackoff, retryPredicate);
    }

    private static boolean defaultRetryPredicate(Throwable throwable) {
        return throwable instanceof RetryableException;
    }

    private static void sleep(Duration duration) {
        if (duration.isZero() || duration.isNegative()) {
            return;
        }
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RetryableException("Retry interrupted", ex);
        }
    }

    private static RuntimeException propagate(Throwable throwable) {
        if (throwable instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new RetryableException("Retry operation failed", throwable);
    }
}
