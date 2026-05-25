package com.pearl.common.test.examples;

import com.pearl.common.config.OpenTelemetryConfig;
import com.pearl.common.config.OpenTelemetryProperties;
import com.pearl.common.correlation.CorrelationContext;
import com.pearl.common.correlation.CorrelationService;
import com.pearl.common.logging.MdcContextManager;
import com.pearl.common.tracing.TraceHelper;
import io.opentelemetry.api.OpenTelemetry;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SampleUsageExamples {

    private static final Logger LOGGER = LoggerFactory.getLogger(SampleUsageExamples.class);

    @Test
    void springBootStyleUsage() {
        CorrelationContext context = CorrelationContext.builder()
                .correlationId(CorrelationService.generateCorrelationId())
                .batchId(CorrelationService.generateBatchId())
                .vendorId("vendor-001")
                .planId("plan-401k")
                .build();

        try (MdcContextManager.MdcScope ignored = MdcContextManager.withContext(context)) {
            LOGGER.info("Processing Spring Boot request");
        }
    }

    @Test
    void lambdaStyleUsage() {
        CorrelationContext context = CorrelationContext.builder()
                .correlationId("lambda-correlation")
                .batchId("lambda-batch")
                .vendorId("vendor-001")
                .planId("plan-401k")
                .requestId("aws-request-id")
                .build();

        CorrelationService.runWithContext(context, () -> LOGGER.info("Processing Lambda event"));
    }

    @Test
    void ecsWorkerTracingUsage() {
        OpenTelemetry openTelemetry = OpenTelemetryConfig.create(new OpenTelemetryProperties(
                        "ecs-worker",
                        "pearl-payroll-platform",
                        "test",
                        "us-east-1",
                        "http://localhost:4317",
                        false,
                        Duration.ofSeconds(5),
                        Map.of()))
                .openTelemetry();
        TraceHelper traceHelper = new TraceHelper(openTelemetry.getTracer("ecs-worker"));

        traceHelper.runInSpan("worker.process-batch", () -> LOGGER.info("Processing ECS worker batch"));
    }
}
