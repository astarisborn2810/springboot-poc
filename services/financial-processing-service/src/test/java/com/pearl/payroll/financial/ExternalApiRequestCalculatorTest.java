package com.pearl.payroll.financial;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ExternalApiRequestCalculatorTest {

    @ParameterizedTest
    @CsvSource({
            "800, 800, 4",
            "300, 200, 1",
            "500, 0, 1",
            "0, 500, 1",
            "200, 300, 1",
            "200, 800, 2"
    })
    void expectedRequestCountUsesCombinedPayrollAndControlTotalRecords(
            int payrollRecordCount,
            int controlTotalRecordCount,
            int expectedRequests) {
        assertEquals(
                expectedRequests,
                ExternalApiRequestCalculator.expectedRequestCount(payrollRecordCount, controlTotalRecordCount));
    }
}
