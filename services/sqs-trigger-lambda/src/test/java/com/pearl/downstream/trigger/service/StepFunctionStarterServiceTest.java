package com.pearl.downstream.trigger.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pearl.downstream.trigger.model.StepFunctionInput;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.sfn.SfnClient;
import software.amazon.awssdk.services.sfn.model.StartExecutionRequest;
import software.amazon.awssdk.services.sfn.model.StartExecutionResponse;

class StepFunctionStarterServiceTest {

    @Test
    void startsStepFunctionExecutionWithSerializedInput() {
        SfnClient sfnClient = mock(SfnClient.class);
        when(sfnClient.startExecution(any(StartExecutionRequest.class)))
                .thenReturn(StartExecutionResponse.builder().executionArn("execution-arn").build());
        StepFunctionStarterService service = new StepFunctionStarterService(sfnClient, "state-machine-arn");
        StepFunctionInput input = new StepFunctionInput(
                "corr-001",
                "batch-20260522",
                "prismhr",
                "financial",
                "payroll-outbound-dev",
                "outbound/prismhr/financial/batch-20260522/file.json",
                "ObjectCreated:Put",
                Instant.parse("2026-05-24T10:00:00Z"),
                "ap-south-1",
                4096L,
                "etag-test",
                "sequencer-test",
                "message-1",
                Map.of("source", "unit-test"));

        String executionArn = service.startExecution(input);

        ArgumentCaptor<StartExecutionRequest> captor = ArgumentCaptor.forClass(StartExecutionRequest.class);
        verify(sfnClient).startExecution(captor.capture());
        StartExecutionRequest request = captor.getValue();
        assertEquals("execution-arn", executionArn);
        assertEquals("state-machine-arn", request.stateMachineArn());
        assertTrue(request.name().length() <= 80);
        assertTrue(request.input().contains("\"correlationId\":\"corr-001\""));
        assertTrue(request.input().contains("\"bucket\":\"payroll-outbound-dev\""));
        assertTrue(request.input().contains("\"key\":\"outbound/prismhr/financial/batch-20260522/file.json\""));
    }
}
