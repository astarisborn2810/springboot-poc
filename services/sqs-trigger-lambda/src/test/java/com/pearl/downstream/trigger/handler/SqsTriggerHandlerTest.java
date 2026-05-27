package com.pearl.downstream.trigger.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.pearl.downstream.trigger.exception.TriggerProcessingException;
import com.pearl.downstream.trigger.logging.JsonLogger;
import com.pearl.downstream.trigger.service.TriggerMessageProcessor;
import java.util.List;
import org.junit.jupiter.api.Test;

class SqsTriggerHandlerTest {

    @Test
    void returnsEmptyBatchResponseForEmptyEvent() {
        SqsTriggerHandler handler = new SqsTriggerHandler(mock(TriggerMessageProcessor.class), new JsonLogger());

        SQSBatchResponse response = handler.handleRequest(new SQSEvent(), null);

        assertTrue(response.getBatchItemFailures().isEmpty());
    }

    @Test
    void delegatesEachSqsRecordToProcessor() {
        TriggerMessageProcessor processor = mock(TriggerMessageProcessor.class);
        SqsTriggerHandler handler = new SqsTriggerHandler(processor, new JsonLogger());
        SQSEvent.SQSMessage first = message("message-1");
        SQSEvent.SQSMessage second = message("message-2");
        SQSEvent event = new SQSEvent();
        event.setRecords(List.of(first, second));

        SQSBatchResponse response = handler.handleRequest(event, null);

        assertTrue(response.getBatchItemFailures().isEmpty());
        verify(processor).process(first);
        verify(processor).process(second);
    }

    @Test
    void returnsOnlyFailedMessageIdForPartialBatchRetry() {
        TriggerMessageProcessor processor = mock(TriggerMessageProcessor.class);
        SQSEvent.SQSMessage message = message("message-1");
        when(processor.process(message)).thenThrow(new TriggerProcessingException("boom"));
        SqsTriggerHandler handler = new SqsTriggerHandler(processor, new JsonLogger());
        SQSEvent event = new SQSEvent();
        event.setRecords(List.of(message));

        SQSBatchResponse response = handler.handleRequest(event, null);

        assertEquals(1, response.getBatchItemFailures().size());
        assertEquals("message-1", response.getBatchItemFailures().getFirst().getItemIdentifier());
    }

    @Test
    void retriesOnlyFailedRecordInMultiRecordBatch() {
        TriggerMessageProcessor processor = mock(TriggerMessageProcessor.class);
        SQSEvent.SQSMessage first = message("message-1");
        SQSEvent.SQSMessage second = message("message-2");
        SQSEvent.SQSMessage third = message("message-3");
        when(processor.process(second)).thenThrow(new TriggerProcessingException("boom"));
        SqsTriggerHandler handler = new SqsTriggerHandler(processor, new JsonLogger());
        SQSEvent event = new SQSEvent();
        event.setRecords(List.of(first, second, third));

        SQSBatchResponse response = handler.handleRequest(event, null);

        assertEquals(1, response.getBatchItemFailures().size());
        assertEquals("message-2", response.getBatchItemFailures().getFirst().getItemIdentifier());
        verify(processor).process(first);
        verify(processor).process(second);
        verify(processor).process(third);
    }

    private static SQSEvent.SQSMessage message(String messageId) {
        SQSEvent.SQSMessage message = new SQSEvent.SQSMessage();
        message.setMessageId(messageId);
        message.setBody("{}");
        return message;
    }
}
