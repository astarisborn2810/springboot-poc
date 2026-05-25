package com.pearl.common.tracing;

import com.pearl.common.correlation.CorrelationContext;
import com.pearl.common.correlation.CorrelationService;
import com.pearl.common.telemetry.TelemetryAttributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;

public final class TraceHelper {

    private final Tracer tracer;

    public TraceHelper(Tracer tracer) {
        this.tracer = Objects.requireNonNull(tracer, "tracer must not be null");
    }

    public SpanScope startSpan(String spanName) {
        Span span = tracer.spanBuilder(spanName).startSpan();
        TelemetryAttributes.enrichSpan(span, CorrelationService.getCurrentOrEmpty());
        return new SpanScope(span, span.makeCurrent());
    }

    public SpanScope startSpan(String spanName, Map<String, ?> attributes) {
        SpanScope scope = startSpan(spanName);
        addAttributes(scope.span(), attributes);
        return scope;
    }

    public <T> T inSpan(String spanName, Callable<T> operation) {
        try (SpanScope spanScope = startSpan(spanName)) {
            try {
                return operation.call();
            } catch (Throwable ex) {
                recordException(spanScope.span(), ex);
                throw propagate(ex);
            }
        }
    }

    public void runInSpan(String spanName, Runnable operation) {
        inSpan(spanName, () -> {
            operation.run();
            return null;
        });
    }

    public Runnable wrapAsync(String spanName, Runnable operation) {
        return wrapRunnable(spanName, operation);
    }

    public <T> Callable<T> wrapAsync(String spanName, Callable<T> operation) {
        return wrapCallable(spanName, operation);
    }

    public Runnable wrapRunnable(String spanName, Runnable operation) {
        Objects.requireNonNull(operation, "operation must not be null");
        Context parent = Context.current();
        CorrelationContext correlationContext = CorrelationService.getCurrentOrEmpty();
        return () -> {
            try (Scope ignored = parent.makeCurrent()) {
                CorrelationService.runWithContext(correlationContext, () -> runInSpan(spanName, operation));
            }
        };
    }

    public <T> Callable<T> wrapCallable(String spanName, Callable<T> operation) {
        Objects.requireNonNull(operation, "operation must not be null");
        Context parent = Context.current();
        CorrelationContext correlationContext = CorrelationService.getCurrentOrEmpty();
        return () -> {
            try (Scope ignored = parent.makeCurrent()) {
                return CorrelationService.callWithContext(correlationContext, () -> inSpan(spanName, operation));
            }
        };
    }

    public static void addAttributes(Span span, Map<String, ?> attributes) {
        if (span == null || attributes == null || attributes.isEmpty()) {
            return;
        }
        attributes.forEach((key, value) -> {
            if (key == null || value == null) {
                return;
            }
            if (value instanceof Boolean booleanValue) {
                span.setAttribute(key, booleanValue);
            } else if (value instanceof Long longValue) {
                span.setAttribute(key, longValue);
            } else if (value instanceof Integer integerValue) {
                span.setAttribute(key, integerValue.longValue());
            } else if (value instanceof Double doubleValue) {
                span.setAttribute(key, doubleValue);
            } else {
                span.setAttribute(key, String.valueOf(value));
            }
        });
    }

    public static void enrichCurrentSpan(CorrelationContext context) {
        TelemetryAttributes.enrichSpan(Span.current(), context);
    }

    public static void recordException(Span span, Throwable throwable) {
        if (span == null || throwable == null) {
            return;
        }
        span.recordException(throwable);
        span.setStatus(StatusCode.ERROR, throwable.getMessage() == null ? "error" : throwable.getMessage());
    }

    private static RuntimeException propagate(Throwable throwable) {
        if (throwable instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new IllegalStateException(throwable);
    }

    public static final class SpanScope implements AutoCloseable {
        private final Span span;
        private final Scope scope;
        private boolean closed;

        private SpanScope(Span span, Scope scope) {
            this.span = span;
            this.scope = scope;
        }

        public Span span() {
            return span;
        }

        @Override
        public void close() {
            if (!closed) {
                try {
                    scope.close();
                } finally {
                    span.end();
                    closed = true;
                }
            }
        }
    }
}
