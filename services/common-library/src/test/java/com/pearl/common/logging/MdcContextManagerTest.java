package com.pearl.common.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.pearl.common.constants.Constants;
import com.pearl.common.correlation.CorrelationContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class MdcContextManagerTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void withContextRestoresPreviousMdcValues() {
        MDC.put(Constants.MdcKeys.CORRELATION_ID, "previous");
        CorrelationContext context = CorrelationContext.builder()
                .correlationId("current")
                .batchId("batch")
                .vendorId("vendor")
                .planId("plan")
                .build();

        try (MdcContextManager.MdcScope ignored = MdcContextManager.withContext(context)) {
            assertEquals("current", MDC.get(Constants.MdcKeys.CORRELATION_ID));
            assertEquals("batch", MDC.get(Constants.MdcKeys.BATCH_ID));
            assertEquals("vendor", MDC.get(Constants.MdcKeys.VENDOR_ID));
            assertEquals("plan", MDC.get(Constants.MdcKeys.PLAN_ID));
        }

        assertEquals("previous", MDC.get(Constants.MdcKeys.CORRELATION_ID));
        assertNull(MDC.get(Constants.MdcKeys.BATCH_ID));
    }

    @Test
    void wrapPropagatesCapturedMdc() {
        MDC.put(Constants.MdcKeys.CORRELATION_ID, "captured");
        Runnable wrapped = MdcContextManager.wrap(() ->
                assertEquals("captured", MDC.get(Constants.MdcKeys.CORRELATION_ID)));
        MDC.clear();

        wrapped.run();

        assertNull(MDC.get(Constants.MdcKeys.CORRELATION_ID));
    }
}
