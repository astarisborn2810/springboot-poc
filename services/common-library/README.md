# PEARL Common Library

Shared Java 24 library for PEARL payroll services, Lambda handlers, ECS workers, and Step Functions integrations.

## Purpose

This module provides a stable platform surface for:

- correlation ID and batch context propagation
- structured CloudWatch-compatible JSON logging
- MDC population and async MDC propagation
- OpenTelemetry SDK setup and OTLP export configuration
- manual tracing helpers
- immutable DTO contracts
- standardized exceptions and retry classification
- reusable JSON, date/time, ID, and retry utilities

Base package:

```text
com.pearl.common
```

## Package Structure

```text
com.pearl.common
|-- config
|-- constants
|-- correlation
|-- dto
|-- exception
|-- logging
|-- telemetry
|-- test
|-- tracing
`-- util
```

## Spring Boot Usage

Register the correlation filter and import the Spring OpenTelemetry config where needed:

```java
import com.pearl.common.config.SpringOpenTelemetryConfiguration;
import com.pearl.common.logging.CorrelationIdFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@Import(SpringOpenTelemetryConfiguration.class)
class PlatformConfig {

    @Bean
    CorrelationIdFilter correlationIdFilter() {
        return new CorrelationIdFilter();
    }
}
```

Incoming headers:

```text
X-Correlation-Id
X-Batch-Id
X-Vendor-Id
X-Plan-Id
X-Execution-Id
X-Request-Id
```

## Lambda Usage

```java
CorrelationContext context = CorrelationContext.builder()
    .correlationId(eventCorrelationId)
    .batchId(batchId)
    .vendorId(vendorId)
    .planId(planId)
    .requestId(context.getAwsRequestId())
    .build();

CorrelationService.runWithContext(context, () -> {
    logger.info("Processing Lambda payload");
});
```

## ECS Worker Usage

```java
OpenTelemetryRuntime runtime = OpenTelemetryConfig.create(OpenTelemetryProperties.fromEnvironment());
TraceHelper traces = new TraceHelper(runtime.openTelemetry().getTracer("financial-processing-service"));

traces.runInSpan("financial.process-batch", () -> {
    logger.info("Processing batch");
});
```

## MDC Logging

Use `MdcContextManager.withContext(...)` for scoped logging context:

```java
try (MdcContextManager.MdcScope ignored = MdcContextManager.withContext(correlationContext)) {
    logger.info("Batch received");
}
```

For async execution:

```java
executor.submit(MdcContextManager.wrap(() -> logger.info("Async step")));
```

Every structured log emitted inside a populated scope includes:

- `correlationId`
- `batchId`
- `vendorId`
- `planId`
- `executionId`
- `requestId`

## Tracing

```java
TraceHelper traceHelper = new TraceHelper(openTelemetry.getTracer("result-tracking-service"));

traceHelper.runInSpan("result.track-batch", () -> {
    TraceHelper.addAttributes(Span.current(), Map.of("record.count", 250));
});
```

The helper enriches spans from `CorrelationService.getCurrentOrEmpty()` using PEARL tracing keys.

## OpenTelemetry Environment Variables

```text
OTEL_SERVICE_NAME=financial-processing-service
OTEL_SERVICE_NAMESPACE=pearl-payroll-platform
OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector:4317
OTEL_EXPORT_ENABLED=true
OTEL_METRIC_EXPORT_INTERVAL_SECONDS=30
PEARL_ENV=dev
AWS_REGION=us-east-1
```

## DTOs

Core immutable records:

- `BaseEvent`
- `BatchExecutionRequest`
- `BatchExecutionResponse`
- `ProcessingResult`
- `ErrorResponse`
- `S3PayloadMetadata`
- `AuditMetadata`

All DTOs include correlation fields, timestamps, and audit metadata.

## Exceptions

Standard exception hierarchy:

- `PearlException`
- `ValidationException`
- `ProcessingException`
- `AwsIntegrationException`
- `RetryableException`
- `NonRetryableException`

Each exception carries:

- standardized error code
- immutable metadata
- retry classification

## Verification

Run unit tests:

```powershell
mvn -pl services/common-library test
```

Run unit and integration tests:

```powershell
mvn -pl services/common-library verify
```
