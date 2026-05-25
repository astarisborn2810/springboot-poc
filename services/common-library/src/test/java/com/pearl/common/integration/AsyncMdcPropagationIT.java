package com.pearl.common.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.pearl.common.constants.Constants;
import com.pearl.common.correlation.CorrelationContext;
import com.pearl.common.logging.MdcContextManager;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class AsyncMdcPropagationIT {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void propagatesMdcAcrossExecutorBoundary() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            MdcContextManager.apply(CorrelationContext.builder()
                    .correlationId("corr-async")
                    .batchId("batch-async")
                    .vendorId("vendor-async")
                    .planId("plan-async")
                    .build());

            Future<String> future = executor.submit(MdcContextManager.wrap(
                    () -> MDC.get(Constants.MdcKeys.CORRELATION_ID)));

            assertEquals("corr-async", future.get());
        } finally {
            executor.shutdownNow();
        }
    }
}
