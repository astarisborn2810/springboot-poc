package com.pearl.common.config;

import com.pearl.common.telemetry.TelemetryAttributes;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import java.util.Map;

public class OpenTelemetryConfig {

    public static OpenTelemetryRuntime create(OpenTelemetryProperties properties) {
        Resource resource = Resource.getDefault().merge(resource(properties));
        SdkTracerProvider tracerProvider = tracerProvider(properties, resource);
        SdkMeterProvider meterProvider = meterProvider(properties, resource);

        OpenTelemetrySdk sdk = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .setMeterProvider(meterProvider)
                .setPropagators(ContextPropagators.create(TextMapPropagator.composite(
                        W3CTraceContextPropagator.getInstance(),
                        W3CBaggagePropagator.getInstance())))
                .build();

        return new OpenTelemetryRuntime(sdk, tracerProvider, meterProvider);
    }

    public static OpenTelemetry initializeGlobal(OpenTelemetryProperties properties) {
        OpenTelemetryRuntime runtime = create(properties);
        GlobalOpenTelemetry.set(runtime.openTelemetry());
        return runtime.openTelemetry();
    }

    private static Resource resource(OpenTelemetryProperties properties) {
        Attributes attributes = Attributes.builder()
                .put(AttributeKey.stringKey(TelemetryAttributes.SERVICE_NAME), properties.serviceName())
                .put(AttributeKey.stringKey(TelemetryAttributes.SERVICE_NAMESPACE), properties.serviceNamespace())
                .put(AttributeKey.stringKey(TelemetryAttributes.DEPLOYMENT_ENVIRONMENT), properties.environment())
                .put(AttributeKey.stringKey(TelemetryAttributes.CLOUD_PROVIDER), "aws")
                .put(AttributeKey.stringKey(TelemetryAttributes.CLOUD_PLATFORM), "aws_ecs")
                .put(AttributeKey.stringKey(TelemetryAttributes.AWS_REGION), properties.region())
                .putAll(toAttributes(properties.resourceAttributes()))
                .build();
        return Resource.create(attributes);
    }

    private static SdkTracerProvider tracerProvider(OpenTelemetryProperties properties, Resource resource) {
        SdkTracerProviderBuilder builder = SdkTracerProvider.builder().setResource(resource);
        if (properties.exportEnabled()) {
            OtlpGrpcSpanExporter spanExporter = OtlpGrpcSpanExporter.builder()
                    .setEndpoint(properties.otlpEndpoint())
                    .build();
            builder.addSpanProcessor(BatchSpanProcessor.builder(spanExporter).build());
        }
        return builder.build();
    }

    private static SdkMeterProvider meterProvider(OpenTelemetryProperties properties, Resource resource) {
        SdkMeterProviderBuilder builder = SdkMeterProvider.builder().setResource(resource);
        if (properties.exportEnabled()) {
            OtlpGrpcMetricExporter metricExporter = OtlpGrpcMetricExporter.builder()
                    .setEndpoint(properties.otlpEndpoint())
                    .build();
            builder.registerMetricReader(PeriodicMetricReader.builder(metricExporter)
                    .setInterval(properties.metricExportInterval())
                    .build());
        }
        return builder.build();
    }

    private static Attributes toAttributes(Map<String, String> attributes) {
        AttributesBuilder builder = Attributes.builder();
        attributes.forEach((key, value) -> builder.put(AttributeKey.stringKey(key), value));
        return builder.build();
    }
}
