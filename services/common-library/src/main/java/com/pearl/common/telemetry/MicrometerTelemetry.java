package com.pearl.common.telemetry;

import com.pearl.common.correlation.CorrelationContext;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Objects;

public final class MicrometerTelemetry {

    private MicrometerTelemetry() {
    }

    public static void incrementBatchCounter(
            MeterRegistry registry,
            String metricName,
            String status,
            CorrelationContext context) {
        Objects.requireNonNull(registry, "registry must not be null");
        Counter.builder(metricName)
                .tag("status", status == null ? "unknown" : status)
                .tag("vendorId", context == null ? "unknown" : context.vendorId().orElse("unknown"))
                .tag("planId", context == null ? "unknown" : context.planId().orElse("unknown"))
                .register(registry)
                .increment();
    }
}
