package com.pearl.common.config;

import com.pearl.common.constants.Constants;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;

public record OpenTelemetryProperties(
        String serviceName,
        String serviceNamespace,
        String environment,
        String region,
        String otlpEndpoint,
        boolean exportEnabled,
        Duration metricExportInterval,
        Map<String, String> resourceAttributes) {

    public OpenTelemetryProperties {
        serviceName = defaultIfBlank(serviceName, Constants.DEFAULT_SERVICE_NAME);
        serviceNamespace = defaultIfBlank(serviceNamespace, Constants.PLATFORM_NAME);
        environment = defaultIfBlank(environment, "local");
        region = defaultIfBlank(region, "us-east-1");
        otlpEndpoint = defaultIfBlank(otlpEndpoint, "http://localhost:4317");
        metricExportInterval = metricExportInterval == null ? Duration.ofSeconds(30) : metricExportInterval;
        resourceAttributes = resourceAttributes == null ? Map.of() : Map.copyOf(resourceAttributes);
    }

    public static OpenTelemetryProperties fromEnvironment() {
        Map<String, String> env = System.getenv();
        return new OpenTelemetryProperties(
                env.getOrDefault("OTEL_SERVICE_NAME", env.getOrDefault("SPRING_APPLICATION_NAME", "pearl-service")),
                env.getOrDefault("OTEL_SERVICE_NAMESPACE", Constants.PLATFORM_NAME),
                env.getOrDefault("PEARL_ENV", env.getOrDefault("ENVIRONMENT", "local")),
                env.getOrDefault("AWS_REGION", "us-east-1"),
                env.getOrDefault("OTEL_EXPORTER_OTLP_ENDPOINT", "http://localhost:4317"),
                Boolean.parseBoolean(env.getOrDefault("OTEL_EXPORT_ENABLED", "true").toLowerCase(Locale.ROOT)),
                Duration.ofSeconds(Long.parseLong(env.getOrDefault("OTEL_METRIC_EXPORT_INTERVAL_SECONDS", "30"))),
                Map.of());
    }

    private static String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
