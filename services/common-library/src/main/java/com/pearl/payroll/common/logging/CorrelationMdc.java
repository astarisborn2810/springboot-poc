package com.pearl.payroll.common.logging;

import com.pearl.payroll.common.constants.PearlConstants;
import com.pearl.payroll.common.correlation.CorrelationContext;
import org.slf4j.MDC;

public final class CorrelationMdc {

    public static final String MDC_CORRELATION_ID = "correlationId";
    public static final String MDC_TENANT_ID = "tenantId";

    private CorrelationMdc() {
    }

    public static void putCorrelationId(String correlationId) {
        CorrelationContext.setCorrelationId(correlationId);
        MDC.put(MDC_CORRELATION_ID, correlationId);
    }

    public static void putTenantId(String tenantId) {
        MDC.put(MDC_TENANT_ID, tenantId == null || tenantId.isBlank()
                ? PearlConstants.DEFAULT_TENANT_ID
                : tenantId);
    }

    public static void clear() {
        CorrelationContext.clear();
        MDC.remove(MDC_CORRELATION_ID);
        MDC.remove(MDC_TENANT_ID);
    }
}
