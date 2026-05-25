package com.pearl.payroll.lambda.datatyperouter;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.pearl.payroll.common.constants.PearlConstants;
import com.pearl.payroll.common.correlation.CorrelationIdGenerator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class DatatypeRouterHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
        String payloadType = String.valueOf(event.getOrDefault("payloadType", "unknown"))
                .toLowerCase(Locale.ROOT);
        String route = switch (payloadType) {
            case PearlConstants.FINANCIAL_PAYLOAD_TYPE -> "financial-processing-service";
            case PearlConstants.INDICATIVE_PAYLOAD_TYPE -> "indicative-processing-service";
            default -> "manual-review";
        };

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("correlationId", event.getOrDefault("correlationId", CorrelationIdGenerator.newCorrelationId()));
        response.put("payloadType", payloadType);
        response.put("route", route);
        response.put("requestId", context == null ? "local" : context.getAwsRequestId());
        return response;
    }
}
