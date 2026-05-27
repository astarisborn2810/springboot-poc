package com.pearl.payroll.financial;

final class ExternalApiRequestCalculator {

    static final int EXTERNAL_API_BATCH_SIZE = 500;

    private ExternalApiRequestCalculator() {
    }

    static int expectedRequestCount(int payrollRecordCount, int controlTotalRecordCount) {
        if (payrollRecordCount < 0 || controlTotalRecordCount < 0) {
            throw new IllegalArgumentException("Record counts must not be negative.");
        }
        int totalRecordCount = payrollRecordCount + controlTotalRecordCount;
        if (totalRecordCount == 0) {
            return 0;
        }
        return (totalRecordCount + EXTERNAL_API_BATCH_SIZE - 1) / EXTERNAL_API_BATCH_SIZE;
    }
}
