package com.pearl.downstream.trigger.service;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.pearl.downstream.trigger.exception.InvalidTriggerMessageException;
import com.pearl.downstream.trigger.exception.TriggerProcessingException;
import com.pearl.downstream.trigger.logging.JsonLogger;
import com.pearl.downstream.trigger.model.CorrelationMetadata;
import com.pearl.downstream.trigger.model.S3EventMessage;
import com.pearl.downstream.trigger.model.StepFunctionInput;
import com.pearl.downstream.trigger.util.JsonUtil;
import com.pearl.downstream.trigger.util.S3ObjectKeyUtil;
import com.pearl.downstream.trigger.validator.MessageValidator;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class TriggerMessageProcessor {

    private final StepFunctionStarterService stepFunctionStarterService;
    private final MessageValidator messageValidator;
    private final CorrelationService correlationService;
    private final JsonLogger jsonLogger;

    public TriggerMessageProcessor() {
        this(new StepFunctionStarterService(), new MessageValidator(), new CorrelationService(), new JsonLogger());
    }

    public TriggerMessageProcessor(
            StepFunctionStarterService stepFunctionStarterService,
            MessageValidator messageValidator,
            CorrelationService correlationService,
            JsonLogger jsonLogger) {
        this.stepFunctionStarterService = stepFunctionStarterService;
        this.messageValidator = messageValidator;
        this.correlationService = correlationService;
        this.jsonLogger = jsonLogger;
    }

    public List<StepFunctionInput> process(SQSEvent.SQSMessage sqsMessage) {
        if (sqsMessage == null) {
            throw new InvalidTriggerMessageException("SQS message is required");
        }
        JsonNode root = JsonUtil.readTree(sqsMessage.getBody());
        if (isS3TestEvent(root)) {
            jsonLogger.warn("Ignoring S3 test event", null);
            return List.of();
        }

        JsonNode records = root.path("Records");
        if (!records.isArray() || records.isEmpty()) {
            throw new InvalidTriggerMessageException("S3 event message must contain at least one Records entry");
        }

        List<StepFunctionInput> inputs = new ArrayList<>();
        for (JsonNode record : records) {
            S3EventMessage eventMessage = toS3EventMessage(record, sqsMessage);
            messageValidator.validate(eventMessage);
            StepFunctionInput input = StepFunctionInput.from(eventMessage);
            String executionArn = stepFunctionStarterService.startExecution(input);
            jsonLogger.info("Started Step Function execution: " + executionArn, eventMessage);
            inputs.add(input);
        }
        return List.copyOf(inputs);
    }

    private S3EventMessage toS3EventMessage(JsonNode record, SQSEvent.SQSMessage sqsMessage) {
        try {
            String rawKey = text(record, "/s3/object/key");
            String decodedKey = S3ObjectKeyUtil.decode(rawKey);
            CorrelationMetadata correlation = correlationService.correlationFor(sqsMessage, decodedKey);
            return new S3EventMessage(
                    text(record, "/s3/bucket/name"),
                    decodedKey,
                    text(record, "/eventName"),
                    instant(record, "/eventTime"),
                    text(record, "/awsRegion"),
                    longValue(record, "/s3/object/size"),
                    text(record, "/s3/object/eTag"),
                    text(record, "/s3/object/sequencer"),
                    sqsMessage.getMessageId(),
                    sqsMessage.getBody(),
                    correlation);
        } catch (InvalidTriggerMessageException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new TriggerProcessingException("Unable to parse S3 event record", ex);
        }
    }

    private static boolean isS3TestEvent(JsonNode root) {
        return root != null
                && root.path("Service").asText("").equals("Amazon S3")
                && root.path("Event").asText("").equals("s3:TestEvent");
    }

    private static String text(JsonNode node, String pointer) {
        JsonNode value = node.at(pointer);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        return value.asText();
    }

    private static Long longValue(JsonNode node, String pointer) {
        JsonNode value = node.at(pointer);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        return value.asLong();
    }

    private static Instant instant(JsonNode node, String pointer) {
        String value = text(node, pointer);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ex) {
            throw new InvalidTriggerMessageException("Invalid S3 eventTime: " + value);
        }
    }
}
