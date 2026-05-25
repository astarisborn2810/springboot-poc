package com.pearl.common.correlation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pearl.common.constants.Constants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class CorrelationServiceTest {

    @AfterEach
    void tearDown() {
        CorrelationService.clear();
    }

    @Test
    void storesCurrentContextAndPopulatesMdc() {
        CorrelationContext context = CorrelationContext.builder()
                .correlationId("corr-1")
                .batchId("batch-1")
                .vendorId("vendor-1")
                .planId("plan-1")
                .executionId("exec-1")
                .requestId("req-1")
                .build();

        CorrelationService.setContext(context);

        assertEquals(context, CorrelationService.currentContext().orElseThrow());
        assertEquals("corr-1", MDC.get(Constants.MdcKeys.CORRELATION_ID));
        assertEquals("batch-1", MDC.get(Constants.MdcKeys.BATCH_ID));
        assertEquals("vendor-1", MDC.get(Constants.MdcKeys.VENDOR_ID));
        assertEquals("plan-1", MDC.get(Constants.MdcKeys.PLAN_ID));
    }

    @Test
    void ensureContextCreatesMissingCorrelationAndBatchIds() {
        CorrelationContext context = CorrelationService.ensureContext();

        assertTrue(context.correlationId().isPresent());
        assertTrue(context.batchId().orElseThrow().startsWith("batch-"));
    }

    @Test
    void wrapRestoresPreviousContextAfterExecution() {
        CorrelationContext previous = CorrelationContext.builder().correlationId("previous").build();
        CorrelationContext captured = CorrelationContext.builder().correlationId("captured").build();
        CorrelationService.setContext(captured);
        Runnable wrapped = CorrelationService.wrap(() ->
                assertEquals("captured", CorrelationService.getCurrentOrEmpty().correlationId().orElseThrow()));

        CorrelationService.setContext(previous);
        wrapped.run();

        assertEquals("previous", CorrelationService.getCurrentOrEmpty().correlationId().orElseThrow());
    }

    @Test
    void generatedIdsAreUnique() {
        assertNotEquals(CorrelationService.generateCorrelationId(), CorrelationService.generateCorrelationId());
        assertNotEquals(CorrelationService.generateBatchId(), CorrelationService.generateBatchId());
    }
}
