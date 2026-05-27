package com.pearl.downstream.trigger.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.pearl.downstream.trigger.exception.TriggerProcessingException;
import com.pearl.downstream.trigger.logging.JsonLogger;
import com.pearl.downstream.trigger.service.TriggerMessageProcessor;
import java.util.ArrayList;
import java.util.List;

public class SqsTriggerHandler implements RequestHandler<SQSEvent, SQSBatchResponse> {

    private final TriggerMessageProcessor triggerMessageProcessor;
    private final JsonLogger jsonLogger;

    public SqsTriggerHandler() {
        this(new TriggerMessageProcessor(), new JsonLogger());
    }

    public SqsTriggerHandler(TriggerMessageProcessor triggerMessageProcessor, JsonLogger jsonLogger) {
        this.triggerMessageProcessor = triggerMessageProcessor;
        this.jsonLogger = jsonLogger;
    }

    @Override
    public SQSBatchResponse handleRequest(SQSEvent event, Context context) {
        List<SQSBatchResponse.BatchItemFailure> failures = new ArrayList<>();
        if (event == null || event.getRecords() == null || event.getRecords().isEmpty()) {
            return new SQSBatchResponse(failures);
        }

        for (SQSEvent.SQSMessage record : event.getRecords()) {
            try {
                triggerMessageProcessor.process(record);
            } catch (TriggerProcessingException ex) {
                jsonLogger.error("Failed to process SQS trigger message", messageId(record), ex);
                failures.add(batchFailure(record));
            } catch (RuntimeException ex) {
                jsonLogger.error("Unexpected failure while processing SQS trigger message", messageId(record), ex);
                failures.add(batchFailure(record));
            }
        }
        return new SQSBatchResponse(failures);
    }

    private static SQSBatchResponse.BatchItemFailure batchFailure(SQSEvent.SQSMessage record) {
        String messageId = messageId(record);
        if (messageId == null || messageId.isBlank()) {
            throw new TriggerProcessingException("Unable to report partial batch failure without an SQS messageId");
        }
        return new SQSBatchResponse.BatchItemFailure(messageId);
    }

    private static String messageId(SQSEvent.SQSMessage record) {
        return record == null ? null : record.getMessageId();
    }
}
