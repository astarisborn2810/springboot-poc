package com.pearl.common.telemetry;

import com.pearl.common.constants.Constants;
import com.pearl.common.correlation.CorrelationContext;
import io.opentelemetry.api.trace.Span;

public final class TelemetryAttributes {

    public static final String SERVICE_NAME = "service.name";
    public static final String SERVICE_NAMESPACE = "service.namespace";
    public static final String DEPLOYMENT_ENVIRONMENT = "deployment.environment";
    public static final String CLOUD_PROVIDER = "cloud.provider";
    public static final String CLOUD_PLATFORM = "cloud.platform";
    public static final String AWS_REGION = "cloud.region";

    private TelemetryAttributes() {
    }

    public static void enrichCurrentSpan(CorrelationContext context) {
        enrichSpan(Span.current(), context);
    }

    public static void enrichSpan(Span span, CorrelationContext context) {
        if (span == null || context == null) {
            return;
        }
        context.correlationId().ifPresent(value -> span.setAttribute(Constants.TracingKeys.CORRELATION_ID, value));
        context.batchId().ifPresent(value -> span.setAttribute(Constants.TracingKeys.BATCH_ID, value));
        context.vendorId().ifPresent(value -> span.setAttribute(Constants.TracingKeys.VENDOR_ID, value));
        context.planId().ifPresent(value -> span.setAttribute(Constants.TracingKeys.PLAN_ID, value));
        context.executionId().ifPresent(value -> span.setAttribute(Constants.TracingKeys.EXECUTION_ID, value));
        context.requestId().ifPresent(value -> span.setAttribute(Constants.TracingKeys.REQUEST_ID, value));
    }
}
