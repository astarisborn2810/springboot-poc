package com.pearl.common.logging;

import com.pearl.common.constants.Constants;
import com.pearl.common.correlation.CorrelationContext;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import org.slf4j.MDC;

public final class MdcContextManager {

    private MdcContextManager() {
    }

    public static void apply(CorrelationContext context) {
        if (context == null) {
            return;
        }
        putIfPresent(Constants.MdcKeys.CORRELATION_ID, context.correlationId().orElse(null));
        putIfPresent(Constants.MdcKeys.BATCH_ID, context.batchId().orElse(null));
        putIfPresent(Constants.MdcKeys.VENDOR_ID, context.vendorId().orElse(null));
        putIfPresent(Constants.MdcKeys.PLAN_ID, context.planId().orElse(null));
        putIfPresent(Constants.MdcKeys.EXECUTION_ID, context.executionId().orElse(null));
        putIfPresent(Constants.MdcKeys.REQUEST_ID, context.requestId().orElse(null));
    }

    public static MdcScope withContext(CorrelationContext context) {
        Map<String, String> previous = capture();
        clearCorrelationKeys();
        apply(context);
        return new MdcScope(previous);
    }

    public static Map<String, String> capture() {
        Map<String, String> copy = MDC.getCopyOfContextMap();
        return copy == null ? Map.of() : Map.copyOf(copy);
    }

    public static void restore(Map<String, String> contextMap) {
        MDC.clear();
        if (contextMap != null && !contextMap.isEmpty()) {
            MDC.setContextMap(new LinkedHashMap<>(contextMap));
        }
    }

    public static Runnable wrap(Runnable delegate) {
        Map<String, String> captured = capture();
        return () -> runWithMdc(captured, delegate);
    }

    public static <T> Callable<T> wrap(Callable<T> delegate) {
        Map<String, String> captured = capture();
        return () -> callWithMdc(captured, delegate);
    }

    public static void runWithMdc(Map<String, String> contextMap, Runnable delegate) {
        callWithMdc(contextMap, () -> {
            delegate.run();
            return null;
        });
    }

    public static <T> T callWithMdc(Map<String, String> contextMap, Callable<T> delegate) {
        Map<String, String> previous = capture();
        try {
            restore(contextMap);
            return delegate.call();
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("MDC-aware callable failed", ex);
        } finally {
            restore(previous);
        }
    }

    public static void clear() {
        MDC.clear();
    }

    public static void clearCorrelationKeys() {
        MDC.remove(Constants.MdcKeys.CORRELATION_ID);
        MDC.remove(Constants.MdcKeys.BATCH_ID);
        MDC.remove(Constants.MdcKeys.VENDOR_ID);
        MDC.remove(Constants.MdcKeys.PLAN_ID);
        MDC.remove(Constants.MdcKeys.EXECUTION_ID);
        MDC.remove(Constants.MdcKeys.REQUEST_ID);
    }

    private static void putIfPresent(String key, String value) {
        if (value == null || value.isBlank()) {
            MDC.remove(key);
        } else {
            MDC.put(key, value);
        }
    }

    public static final class MdcScope implements AutoCloseable {
        private final Map<String, String> previousContext;
        private boolean closed;

        private MdcScope(Map<String, String> previousContext) {
            this.previousContext = previousContext;
        }

        @Override
        public void close() {
            if (!closed) {
                restore(previousContext);
                closed = true;
            }
        }
    }
}
