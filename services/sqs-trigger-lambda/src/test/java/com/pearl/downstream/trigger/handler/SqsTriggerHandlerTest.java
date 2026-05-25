package com.pearl.downstream.trigger.handler;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.pearl.downstream.trigger.exception.TriggerProcessingException;
import com.pearl.downstream.trigger.logging.JsonLogger;
import com.pearl.downstream.trigger.service.TriggerMessageProcessor;
import java.util.List;
import org.junit.jupiter.api.Test;

class SqsTriggerHandlerTest {

    @Test
    void returnsNullForEmptyEvent() {
        SqsTriggerHandler handler = new SqsTriggerHandler(mock(TriggerMessageProcessor.class), new JsonLogger());

        assertNull(handler.handleRequest(new SQSEvent(), null));
    }

    @Test
    void delegatesEachSqsRecordToProcessor() {
        TriggerMessageProcessor processor = mock(TriggerMessageProcessor.class);
        SqsTriggerHandler handler = new SqsTriggerHandler(processor, new JsonLogger());
        SQSEvent.SQSMessage first = message("message-1");
        SQSEvent.SQSMessage second = message("message-2");
        SQSEvent event = new SQSEvent();
        event.setRecords(List.of(first, second));

        assertNull(handler.handleRequest(event, null));

        verify(processor).process(first);
        verify(processor).process(second);
    }

    @Test
    void rethrowsProcessingExceptionSoLambdaRetriesBatch() {
        TriggerMessageProcessor processor = mock(TriggerMessageProcessor.class);
        SQSEvent.SQSMessage message = message("message-1");
        when(processor.process(message)).thenThrow(new TriggerProcessingException("boom"));
        SqsTriggerHandler handler = new SqsTriggerHandler(processor, new JsonLogger());
        SQSEvent event = new SQSEvent();
        event.setRecords(List.of(message));

        assertThrows(TriggerProcessingException.class, () -> handler.handleRequest(event, null));
    }

    private static SQSEvent.SQSMessage message(String messageId) {
        SQSEvent.SQSMessage message = new SQSEvent.SQSMessage();
        message.setMessageId(messageId);
        message.setBody("{}");
        return message;
    }
}
