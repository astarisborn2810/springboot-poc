package com.pearl.payroll.common;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pearl.payroll.common.correlation.CorrelationIdGenerator;
import org.junit.jupiter.api.Test;

class CorrelationIdGeneratorTest {

    @Test
    void createsEnterpriseCorrelationId() {
        assertTrue(CorrelationIdGenerator.newCorrelationId().startsWith("corr-"));
    }
}
