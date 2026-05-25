package com.pearl.payroll.common.logging;

import com.pearl.payroll.common.constants.PearlConstants;
import com.pearl.payroll.common.correlation.CorrelationIdGenerator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

public class CorrelationIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String correlationId = resolveCorrelationId(request);
        String tenantId = request.getHeader(PearlConstants.TENANT_ID_HEADER);

        try {
            CorrelationMdc.putCorrelationId(correlationId);
            CorrelationMdc.putTenantId(tenantId);
            response.setHeader(PearlConstants.CORRELATION_ID_HEADER, correlationId);
            filterChain.doFilter(request, response);
        } finally {
            CorrelationMdc.clear();
        }
    }

    private String resolveCorrelationId(HttpServletRequest request) {
        String candidate = request.getHeader(PearlConstants.CORRELATION_ID_HEADER);
        return candidate == null || candidate.isBlank()
                ? CorrelationIdGenerator.newCorrelationId()
                : candidate.trim();
    }
}
