package com.pearl.common.correlation;

import com.pearl.common.logging.MdcContextManager;
import com.pearl.common.util.IdGeneratorUtil;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

public final class CorrelationService {

    private static final ThreadLocal<CorrelationContext> CURRENT_CONTEXT =
            ThreadLocal.withInitial(CorrelationContext::empty);

    private CorrelationService() {
    }

    public static String generateCorrelationId() {
        return IdGeneratorUtil.uuid();
    }

    public static String generateBatchId() {
        return IdGeneratorUtil.batchId();
    }

    public static CorrelationContext createContext() {
        return CorrelationContext.builder()
                .correlationId(generateCorrelationId())
                .batchId(generateBatchId())
                .build();
    }

    public static CorrelationContext ensureContext() {
        CorrelationContext current = CURRENT_CONTEXT.get();
        if (current == null || current.isEmpty() || current.correlationId().isEmpty()) {
            current = current == null ? CorrelationContext.empty() : current;
            current = current.toBuilder()
                    .correlationId(generateCorrelationId())
                    .batchId(current.batchId().orElseGet(CorrelationService::generateBatchId))
                    .build();
            setContext(current);
        }
        return current;
    }

    public static Optional<CorrelationContext> currentContext() {
        CorrelationContext context = CURRENT_CONTEXT.get();
        return context == null || context.isEmpty() ? Optional.empty() : Optional.of(context);
    }

    public static CorrelationContext getCurrentOrEmpty() {
        return CURRENT_CONTEXT.get();
    }

    public static void setContext(CorrelationContext context) {
        CorrelationContext safeContext = context == null ? CorrelationContext.empty() : context;
        CURRENT_CONTEXT.set(safeContext);
        MdcContextManager.apply(safeContext);
    }

    public static CorrelationContext update(Supplier<CorrelationContext> contextSupplier) {
        CorrelationContext context = contextSupplier == null ? CorrelationContext.empty() : contextSupplier.get();
        setContext(context);
        return context;
    }

    public static void clear() {
        CURRENT_CONTEXT.remove();
        MdcContextManager.clear();
    }

    public static Runnable wrap(Runnable delegate) {
        CorrelationContext captured = getCurrentOrEmpty();
        return () -> runWithContext(captured, delegate);
    }

    public static <T> Callable<T> wrap(Callable<T> delegate) {
        CorrelationContext captured = getCurrentOrEmpty();
        return () -> callWithContext(captured, delegate);
    }

    public static <T> Supplier<T> wrap(Supplier<T> delegate) {
        CorrelationContext captured = getCurrentOrEmpty();
        return () -> callWithContext(captured, delegate::get);
    }

    public static void runWithContext(CorrelationContext context, Runnable delegate) {
        callWithContext(context, () -> {
            delegate.run();
            return null;
        });
    }

    public static <T> T callWithContext(CorrelationContext context, Callable<T> delegate) {
        CorrelationContext previous = getCurrentOrEmpty();
        try (MdcContextManager.MdcScope ignored = MdcContextManager.withContext(context)) {
            CURRENT_CONTEXT.set(context == null ? CorrelationContext.empty() : context);
            return delegate.call();
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Correlation-aware callable failed", ex);
        } finally {
            setContext(previous);
        }
    }
}
