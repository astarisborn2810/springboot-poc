package com.pearl.payroll.common.constants;

public final class PearlConstants {

    public static final String PLATFORM_NAME = "pearl-payroll-platform";
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String CAUSATION_ID_HEADER = "X-Causation-Id";
    public static final String TENANT_ID_HEADER = "X-Tenant-Id";
    public static final String DEFAULT_TENANT_ID = "pearl";

    public static final String FINANCIAL_PAYLOAD_TYPE = "financial";
    public static final String INDICATIVE_PAYLOAD_TYPE = "indicative";

    private PearlConstants() {
    }
}
