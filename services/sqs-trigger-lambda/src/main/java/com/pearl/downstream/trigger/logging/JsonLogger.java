package com.pearl.downstream.trigger.logging;

import com.pearl.downstream.trigger.model.CorrelationMetadata;
import com.pearl.downstream.trigger.model.S3EventMessage;
import com.pearl.downstream.trigger.util.JsonUtil;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonLogger.class);

    public void info(String message, S3EventMessage eventMessage) {
        LOGGER.info(toJson("INFO", message, eventMessage, null));
    }

    public void warn(String message, S3EventMessage eventMessage) {
        LOGGER.warn(toJson("WARN", message, eventMessage, null));
    }

    public void error(String message, S3EventMessage eventMessage, Throwable error) {
        LOGGER.error(toJson("ERROR", message, eventMessage, error), error);
    }

    public void error(String message, String messageId, Throwable error) {
        Map<String, Object> fields = baseFields("ERROR", message);
        fields.put("sourceMessageId", messageId);
        fields.put("exceptionType", error.getClass().getName());
        fields.put("exceptionMessage", error.getMessage());
        LOGGER.error(JsonUtil.toJson(fields), error);
    }

    private static String toJson(String level, String message, S3EventMessage eventMessage, Throwable error) {
        Map<String, Object> fields = baseFields(level, message);
        if (eventMessage != null) {
            CorrelationMetadata correlation = eventMessage.correlation();
            fields.put("correlationId", correlation == null ? null : correlation.correlationId());
            fields.put("batchId", correlation == null ? null : correlation.batchId());
            fields.put("vendorId", correlation == null ? null : correlation.vendorId());
            fields.put("dataType", correlation == null ? null : correlation.dataType());
            fields.put("bucket", eventMessage.bucketName());
            fields.put("key", eventMessage.objectKey());
            fields.put("eventType", eventMessage.eventType());
            fields.put("sourceMessageId", eventMessage.sourceMessageId());
        }
        if (error != null) {
            fields.put("exceptionType", error.getClass().getName());
            fields.put("exceptionMessage", error.getMessage());
        }
        return JsonUtil.toJson(fields);
    }

    private static Map<String, Object> baseFields(String level, String message) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("timestamp", Instant.now().toString());
        fields.put("level", level);
        fields.put("service", "sqs-trigger-lambda");
        fields.put("message", message);
        return fields;
    }
}
