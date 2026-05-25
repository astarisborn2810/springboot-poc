package com.pearl.downstream.trigger.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.pearl.downstream.trigger.exception.TriggerProcessingException;
import com.pearl.downstream.trigger.logging.JsonLogger;
import com.pearl.downstream.trigger.service.TriggerMessageProcessor;

public class SqsTriggerHandler implements RequestHandler<SQSEvent, Void> {

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
    public Void handleRequest(SQSEvent event, Context context) {
        if (event == null || event.getRecords() == null || event.getRecords().isEmpty()) {
            return null;
        }

        for (SQSEvent.SQSMessage record : event.getRecords()) {
            try {
                triggerMessageProcessor.process(record);
            } catch (TriggerProcessingException ex) {
                jsonLogger.error("Failed to process SQS trigger message", record.getMessageId(), ex);
                throw ex;
            } catch (RuntimeException ex) {
                jsonLogger.error("Unexpected failure while processing SQS trigger message", record.getMessageId(), ex);
                throw new TriggerProcessingException("Unexpected failure while processing SQS trigger message", ex);
            }
        }
        return null;
    }
}
