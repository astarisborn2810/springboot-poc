package com.pearl.common.test;

import com.pearl.common.correlation.CorrelationContext;
import com.pearl.common.util.IdGeneratorUtil;

public final class CommonLibraryTestSupport {

    private CommonLibraryTestSupport() {
    }

    public static CorrelationContext testCorrelationContext() {
        return CorrelationContext.builder()
                .correlationId(IdGeneratorUtil.uuid())
                .batchId("batch-test")
                .vendorId("vendor-test")
                .planId("plan-test")
                .executionId("execution-test")
                .requestId("request-test")
                .build();
    }
}
