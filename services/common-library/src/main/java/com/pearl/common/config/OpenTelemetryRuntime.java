package com.pearl.common.config;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.trace.SdkTracerProvider;

public final class OpenTelemetryRuntime implements AutoCloseable {

    private final OpenTelemetrySdk openTelemetrySdk;
    private final SdkTracerProvider tracerProvider;
    private final SdkMeterProvider meterProvider;

    OpenTelemetryRuntime(
            OpenTelemetrySdk openTelemetrySdk,
            SdkTracerProvider tracerProvider,
            SdkMeterProvider meterProvider) {
        this.openTelemetrySdk = openTelemetrySdk;
        this.tracerProvider = tracerProvider;
        this.meterProvider = meterProvider;
    }

    public OpenTelemetry openTelemetry() {
        return openTelemetrySdk;
    }

    public SdkTracerProvider tracerProvider() {
        return tracerProvider;
    }

    public SdkMeterProvider meterProvider() {
        return meterProvider;
    }

    @Override
    public void close() {
        tracerProvider.close();
        meterProvider.close();
    }
}
