package com.pearl.payroll.common.observability;

import com.pearl.payroll.common.constants.PearlConstants;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Tracer;

public final class TelemetrySupport {

    private TelemetrySupport() {
    }

    public static Tracer tracer(String instrumentationScope) {
        String scope = instrumentationScope == null || instrumentationScope.isBlank()
                ? PearlConstants.PLATFORM_NAME
                : instrumentationScope;
        return GlobalOpenTelemetry.getTracer(scope);
    }
}
