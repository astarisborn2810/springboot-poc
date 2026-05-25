package com.pearl.common.logging;

import com.pearl.common.constants.Constants;
import com.pearl.common.correlation.CorrelationContext;
import com.pearl.common.correlation.CorrelationService;
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
        CorrelationContext context = CorrelationContext.builder()
                .correlationId(resolveHeader(request, Constants.Headers.CORRELATION_ID,
                        CorrelationService.generateCorrelationId()))
                .batchId(resolveHeader(request, Constants.Headers.BATCH_ID, null))
                .vendorId(resolveHeader(request, Constants.Headers.VENDOR_ID, null))
                .planId(resolveHeader(request, Constants.Headers.PLAN_ID, null))
                .executionId(resolveHeader(request, Constants.Headers.EXECUTION_ID, null))
                .requestId(resolveHeader(request, Constants.Headers.REQUEST_ID, request.getRequestId()))
                .build();

        try {
            CorrelationService.setContext(context);
            context.correlationId().ifPresent(value -> response.setHeader(Constants.Headers.CORRELATION_ID, value));
            context.batchId().ifPresent(value -> response.setHeader(Constants.Headers.BATCH_ID, value));
            filterChain.doFilter(request, response);
        } finally {
            CorrelationService.clear();
        }
    }

    private static String resolveHeader(HttpServletRequest request, String headerName, String fallback) {
        String value = request.getHeader(headerName);
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
