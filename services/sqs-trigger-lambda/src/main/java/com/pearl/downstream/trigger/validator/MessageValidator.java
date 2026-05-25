package com.pearl.downstream.trigger.validator;

import com.pearl.downstream.trigger.exception.InvalidTriggerMessageException;
import com.pearl.downstream.trigger.model.S3EventMessage;

public class MessageValidator {

    public void validate(S3EventMessage message) {
        if (message == null) {
            throw new InvalidTriggerMessageException("S3 event message is required");
        }
        validateText("bucketName", message.bucketName());
        validateText("objectKey", message.objectKey());
        validateText("eventType", message.eventType());
        if (!message.eventType().startsWith("ObjectCreated:")) {
            throw new InvalidTriggerMessageException("Unsupported S3 event type: " + message.eventType());
        }
    }

    private static void validateText(String field, String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidTriggerMessageException("Missing required S3 event field: " + field);
        }
    }
}
