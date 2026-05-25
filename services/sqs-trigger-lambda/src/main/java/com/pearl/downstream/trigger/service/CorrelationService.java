package com.pearl.downstream.trigger.service;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.pearl.downstream.trigger.model.CorrelationMetadata;
import com.pearl.downstream.trigger.util.S3ObjectKeyUtil;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class CorrelationService {

    public CorrelationMetadata correlationFor(SQSEvent.SQSMessage sqsMessage, String objectKey) {
        Map<String, SQSEvent.MessageAttribute> attributes = sqsMessage == null ? Map.of() : sqsMessage.getMessageAttributes();
        String correlationId = attribute(attributes, "correlationId").orElseGet(() -> UUID.randomUUID().toString());
        String batchId = attribute(attributes, "batchId").or(() -> batchIdFromKey(objectKey)).orElse("unknown-batch");
        String vendorId = attribute(attributes, "vendorId").or(() -> vendorIdFromKey(objectKey)).orElse("unknown-vendor");
        String dataType = attribute(attributes, "dataType").or(() -> dataTypeFromKey(objectKey)).orElse("unknown");

        return new CorrelationMetadata(correlationId, batchId, vendorId, dataType);
    }

    private static Optional<String> attribute(Map<String, SQSEvent.MessageAttribute> attributes, String name) {
        if (attributes == null || attributes.isEmpty()) {
            return Optional.empty();
        }
        SQSEvent.MessageAttribute exact = attributes.get(name);
        if (exact != null && exact.getStringValue() != null && !exact.getStringValue().isBlank()) {
            return Optional.of(exact.getStringValue());
        }
        String lowerName = name.toLowerCase(Locale.ROOT);
        return attributes.entrySet().stream()
                .filter(entry -> entry.getKey() != null && entry.getKey().toLowerCase(Locale.ROOT).equals(lowerName))
                .map(Map.Entry::getValue)
                .map(SQSEvent.MessageAttribute::getStringValue)
                .filter(value -> value != null && !value.isBlank())
                .findFirst();
    }

    private static Optional<String> vendorIdFromKey(String key) {
        return S3ObjectKeyUtil.segment(key, 1)
                .filter(value -> !value.equalsIgnoreCase("financial"))
                .filter(value -> !value.equalsIgnoreCase("indicative"));
    }

    private static Optional<String> dataTypeFromKey(String key) {
        return S3ObjectKeyUtil.segment(key, 2)
                .or(() -> S3ObjectKeyUtil.segment(key, 1))
                .flatMap(S3ObjectKeyUtil::normalizeDataType);
    }

    private static Optional<String> batchIdFromKey(String key) {
        return S3ObjectKeyUtil.segment(key, 3)
                .filter(CorrelationService::looksLikeBatch)
                .or(() -> S3ObjectKeyUtil.segment(key, 2).filter(CorrelationService::looksLikeBatch))
                .or(() -> S3ObjectKeyUtil.segment(key, 1).filter(CorrelationService::looksLikeBatch));
    }

    private static boolean looksLikeBatch(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        return normalized.startsWith("batch") || normalized.matches(".*\\d{8,}.*");
    }
}
