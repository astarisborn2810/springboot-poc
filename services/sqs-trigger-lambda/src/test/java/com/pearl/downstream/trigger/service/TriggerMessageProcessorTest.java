package com.pearl.downstream.trigger.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.pearl.downstream.trigger.exception.InvalidTriggerMessageException;
import com.pearl.downstream.trigger.exception.TriggerProcessingException;
import com.pearl.downstream.trigger.idempotency.IdempotencyClaim;
import com.pearl.downstream.trigger.idempotency.IdempotencyStore;
import com.pearl.downstream.trigger.logging.JsonLogger;
import com.pearl.downstream.trigger.model.StepFunctionInput;
import com.pearl.downstream.trigger.model.StepFunctionStartResult;
import com.pearl.downstream.trigger.validator.MessageValidator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class TriggerMessageProcessorTest {

    @Test
    void ignoresS3TestEventSafely() {
        StepFunctionStarterService starterService = mock(StepFunctionStarterService.class);
        TriggerMessageProcessor processor = processor(starterService);

        List<StepFunctionInput> inputs = processor.process(message("message-1", """
                {
                  "Service": "Amazon S3",
                  "Event": "s3:TestEvent",
                  "Time": "2026-05-24T10:00:00.000Z",
                  "Bucket": "payroll-outbound-dev"
                }
                """));

        assertTrue(inputs.isEmpty());
        verifyNoInteractions(starterService);
    }

    @Test
    void parsesObjectCreatedEventAndStartsStepFunction() {
        StepFunctionStarterService starterService = mock(StepFunctionStarterService.class);
        when(starterService.startExecution(any(StepFunctionInput.class)))
                .thenReturn(StepFunctionStartResult.started("execution-name", "execution-arn"));
        TriggerMessageProcessor processor = processor(starterService);

        List<StepFunctionInput> inputs = processor.process(message("message-2", objectCreatedBody()));

        ArgumentCaptor<StepFunctionInput> captor = ArgumentCaptor.forClass(StepFunctionInput.class);
        verify(starterService).startExecution(captor.capture());
        StepFunctionInput input = captor.getValue();
        assertEquals(1, inputs.size());
        assertEquals("payroll-outbound-dev", input.bucket());
        assertEquals("outbound/prismhr/financial/batch-20260522/batch-20260522_prismhr_PEARL-401K-PLAN-001.json", input.key());
        assertEquals("batch-20260522_prismhr_PEARL-401K-PLAN-001", input.fileName());
        assertEquals("s3://payroll-outbound-dev/outbound/prismhr/financial/batch-20260522/batch-20260522_prismhr_PEARL-401K-PLAN-001.json", input.s3PathOrArn());
        assertEquals("ObjectCreated:Put", input.eventType());
        assertEquals("financial", input.dataType());
        assertEquals("financial", input.payloadType());
        assertEquals("prismhr", input.vendorId());
        assertEquals("batch-20260522", input.batchId());
        assertFalse(input.correlationId().isBlank());
        assertTrue(input.metadata().get("idempotencyKey").startsWith("s3event#"));
    }

    @Test
    void processesMultipleS3RecordsFromOneSqsMessage() {
        StepFunctionStarterService starterService = mock(StepFunctionStarterService.class);
        when(starterService.startExecution(any(StepFunctionInput.class)))
                .thenReturn(StepFunctionStartResult.started("execution-name", "execution-arn"));
        TriggerMessageProcessor processor = processor(starterService);

        List<StepFunctionInput> inputs = processor.process(message("message-multi", objectCreatedBodyWithTwoRecords()));

        ArgumentCaptor<StepFunctionInput> captor = ArgumentCaptor.forClass(StepFunctionInput.class);
        verify(starterService, times(2)).startExecution(captor.capture());
        assertEquals(2, inputs.size());
        assertEquals("batch-20260522_prismhr_PEARL-401K-PLAN-001", inputs.get(0).fileName());
        assertEquals("batch-20260522_prismhr_PEARL-401K-PLAN-002", inputs.get(1).fileName());
        assertEquals(2, captor.getAllValues().size());
    }

    @Test
    void rejectsInvalidS3EventMissingBucket() {
        StepFunctionStarterService starterService = mock(StepFunctionStarterService.class);
        TriggerMessageProcessor processor = processor(starterService);

        assertThrows(InvalidTriggerMessageException.class, () -> processor.process(message("message-3", """
                {
                  "Records": [{
                    "eventName": "ObjectCreated:Put",
                    "eventTime": "2026-05-24T10:00:00.000Z",
                    "s3": {
                      "bucket": {},
                      "object": {
                        "key": "outbound/prismhr/financial/batch-20260522/batch-20260522_prismhr_PEARL-401K-PLAN-001.json"
                      }
                    }
                  }]
                }
                """)));
        verifyNoInteractions(starterService);
    }

    @Test
    void createsStepFunctionInputWithCorrelationAttributesWhenPresent() {
        StepFunctionStarterService starterService = mock(StepFunctionStarterService.class);
        when(starterService.startExecution(any(StepFunctionInput.class)))
                .thenReturn(StepFunctionStartResult.started("execution-name", "execution-arn"));
        TriggerMessageProcessor processor = processor(starterService);
        SQSEvent.SQSMessage message = message("message-4", objectCreatedBody());
        message.setMessageAttributes(Map.of(
                "correlationId", attribute("corr-test-001"),
                "batchId", attribute("batch-override"),
                "vendorId", attribute("vendor-override"),
                "dataType", attribute("indicative")));

        processor.process(message);

        ArgumentCaptor<StepFunctionInput> captor = ArgumentCaptor.forClass(StepFunctionInput.class);
        verify(starterService).startExecution(captor.capture());
        StepFunctionInput input = captor.getValue();
        assertEquals("corr-test-001", input.correlationId());
        assertEquals("batch-override", input.batchId());
        assertEquals("vendor-override", input.vendorId());
        assertEquals("indicative", input.dataType());
        assertEquals("message-4", input.sourceMessageId());
    }

    @Test
    void skipsDuplicateCompletedS3EventWithoutStartingStepFunction() {
        StepFunctionStarterService starterService = mock(StepFunctionStarterService.class);
        IdempotencyStore idempotencyStore = mock(IdempotencyStore.class);
        when(idempotencyStore.claim(any(), any()))
                .thenReturn(IdempotencyClaim.duplicateCompleted("s3event#duplicate", "STARTED"));
        TriggerMessageProcessor processor = processor(starterService, idempotencyStore);

        List<StepFunctionInput> inputs = processor.process(message("message-5", objectCreatedBody()));

        assertEquals(1, inputs.size());
        verify(starterService, never()).startExecution(any(StepFunctionInput.class));
    }

    @Test
    void failsInProgressS3EventSoOnlyThatSqsMessageRetries() {
        StepFunctionStarterService starterService = mock(StepFunctionStarterService.class);
        IdempotencyStore idempotencyStore = mock(IdempotencyStore.class);
        when(idempotencyStore.claim(any(), any()))
                .thenReturn(IdempotencyClaim.inProgress("s3event#in-progress", "STARTING"));
        TriggerMessageProcessor processor = processor(starterService, idempotencyStore);

        assertThrows(TriggerProcessingException.class, () -> processor.process(message("message-6", objectCreatedBody())));
        verify(starterService, never()).startExecution(any(StepFunctionInput.class));
    }

    private static TriggerMessageProcessor processor(StepFunctionStarterService starterService) {
        return new TriggerMessageProcessor(starterService, new MessageValidator(), new CorrelationService(), new JsonLogger());
    }

    private static TriggerMessageProcessor processor(
            StepFunctionStarterService starterService,
            IdempotencyStore idempotencyStore) {
        return new TriggerMessageProcessor(
                starterService,
                new MessageValidator(),
                new CorrelationService(),
                idempotencyStore,
                new JsonLogger());
    }

    private static SQSEvent.SQSMessage message(String messageId, String body) {
        SQSEvent.SQSMessage message = new SQSEvent.SQSMessage();
        message.setMessageId(messageId);
        message.setBody(body);
        return message;
    }

    private static SQSEvent.MessageAttribute attribute(String value) {
        SQSEvent.MessageAttribute attribute = new SQSEvent.MessageAttribute();
        attribute.setDataType("String");
        attribute.setStringValue(value);
        return attribute;
    }

    private static String objectCreatedBody() {
        return """
                {
                  "Records": [{
                    "eventVersion": "2.1",
                    "eventSource": "aws:s3",
                    "awsRegion": "ap-south-1",
                    "eventTime": "2026-05-24T10:00:00.000Z",
                    "eventName": "ObjectCreated:Put",
                    "s3": {
                      "bucket": {
                        "name": "payroll-outbound-dev",
                        "arn": "arn:aws:s3:::payroll-outbound-dev"
                      },
                      "object": {
                        "key": "outbound/prismhr/financial/batch-20260522/batch-20260522_prismhr_PEARL-401K-PLAN-001.json",
                        "size": 4096,
                        "eTag": "etag-test",
                        "sequencer": "00664F1D2A5A"
                      }
                    }
                  }]
                }
                """;
    }

    private static String objectCreatedBodyWithTwoRecords() {
        return """
                {
                  "Records": [
                    {
                      "eventVersion": "2.1",
                      "eventSource": "aws:s3",
                      "awsRegion": "ap-south-1",
                      "eventTime": "2026-05-24T10:00:00.000Z",
                      "eventName": "ObjectCreated:Put",
                      "s3": {
                        "bucket": {
                          "name": "payroll-outbound-dev",
                          "arn": "arn:aws:s3:::payroll-outbound-dev"
                        },
                        "object": {
                          "key": "outbound/prismhr/financial/batch-20260522/batch-20260522_prismhr_PEARL-401K-PLAN-001.json",
                          "size": 4096,
                          "eTag": "etag-test-1",
                          "sequencer": "00664F1D2A5A"
                        }
                      }
                    },
                    {
                      "eventVersion": "2.1",
                      "eventSource": "aws:s3",
                      "awsRegion": "ap-south-1",
                      "eventTime": "2026-05-24T10:00:01.000Z",
                      "eventName": "ObjectCreated:Put",
                      "s3": {
                        "bucket": {
                          "name": "payroll-outbound-dev",
                          "arn": "arn:aws:s3:::payroll-outbound-dev"
                        },
                        "object": {
                          "key": "outbound/prismhr/financial/batch-20260522/batch-20260522_prismhr_PEARL-401K-PLAN-002.json",
                          "size": 8192,
                          "eTag": "etag-test-2",
                          "sequencer": "00664F1D2A5B"
                        }
                      }
                    }
                  ]
                }
                """;
    }
}
