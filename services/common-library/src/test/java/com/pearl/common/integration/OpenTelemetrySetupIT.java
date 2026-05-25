package com.pearl.common.integration;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.pearl.common.config.OpenTelemetryConfig;
import com.pearl.common.config.OpenTelemetryProperties;
import com.pearl.common.config.OpenTelemetryRuntime;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OpenTelemetrySetupIT {

    @Test
    void createsSdkRuntimeWithoutExporterForLocalTests() {
        OpenTelemetryProperties properties = new OpenTelemetryProperties(
                "common-library-it",
                "pearl-payroll-platform",
                "test",
                "us-east-1",
                "http://localhost:4317",
                false,
                Duration.ofSeconds(5),
                Map.of("test.suite", "common-library"));

        try (OpenTelemetryRuntime runtime = OpenTelemetryConfig.create(properties)) {
            assertNotNull(runtime.openTelemetry().getTracer("it"));
            assertNotNull(runtime.openTelemetry().getMeter("it"));
            assertNotNull(runtime.tracerProvider());
            assertNotNull(runtime.meterProvider());
        }
    }
}
