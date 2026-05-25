package com.pearl.common.tracing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.pearl.common.correlation.CorrelationContext;
import com.pearl.common.correlation.CorrelationService;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TraceHelperTest {

    @AfterEach
    void tearDown() {
        CorrelationService.clear();
    }

    @Test
    void createsAndEnrichesSpan() {
        CapturingSpanExporter exporter = new CapturingSpanExporter();
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(exporter))
                .build();
        Tracer tracer = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .build()
                .getTracer("test");
        TraceHelper traceHelper = new TraceHelper(tracer);
        CorrelationService.setContext(CorrelationContext.builder()
                .correlationId("corr-1")
                .batchId("batch-1")
                .vendorId("vendor-1")
                .planId("plan-1")
                .build());

        traceHelper.runInSpan("unit-span", () -> TraceHelper.addAttributes(
                io.opentelemetry.api.trace.Span.current(),
                Map.of("record.count", 10)));

        tracerProvider.close();
        assertFalse(exporter.spans.isEmpty());
        SpanData span = exporter.spans.getFirst();
        assertEquals("unit-span", span.getName());
        assertEquals("corr-1", span.getAttributes().get(io.opentelemetry.api.common.AttributeKey.stringKey("pearl.correlation.id")));
    }

    @Test
    void wrapCallablePropagatesCorrelationAndCreatesSpan() throws Exception {
        CapturingSpanExporter exporter = new CapturingSpanExporter();
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(exporter))
                .build();
        TraceHelper traceHelper = new TraceHelper(OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .build()
                .getTracer("test"));
        CorrelationService.setContext(CorrelationContext.builder().correlationId("corr-async").build());

        Callable<String> callable = traceHelper.wrapCallable(
                "async-callable",
                () -> CorrelationService.getCurrentOrEmpty().correlationId().orElseThrow());

        assertEquals("corr-async", callable.call());
        tracerProvider.close();
        assertEquals("async-callable", exporter.spans.getFirst().getName());
    }

    private static final class CapturingSpanExporter implements SpanExporter {
        private final List<SpanData> spans = new ArrayList<>();

        @Override
        public CompletableResultCode export(Collection<SpanData> spans) {
            this.spans.addAll(spans);
            return CompletableResultCode.ofSuccess();
        }

        @Override
        public CompletableResultCode flush() {
            return CompletableResultCode.ofSuccess();
        }

        @Override
        public CompletableResultCode shutdown() {
            return CompletableResultCode.ofSuccess();
        }
    }
}
