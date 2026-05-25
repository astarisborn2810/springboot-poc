package com.pearl.payroll.common.correlation;

import java.util.Optional;

public final class CorrelationContext {

    private static final ThreadLocal<String> CORRELATION_ID = new ThreadLocal<>();

    private CorrelationContext() {
    }

    public static Optional<String> currentCorrelationId() {
        return Optional.ofNullable(CORRELATION_ID.get());
    }

    public static void setCorrelationId(String correlationId) {
        CORRELATION_ID.set(correlationId);
    }

    public static void clear() {
        CORRELATION_ID.remove();
    }
}
