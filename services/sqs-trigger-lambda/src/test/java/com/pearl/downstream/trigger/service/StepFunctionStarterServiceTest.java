package com.pearl.downstream.trigger.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pearl.downstream.trigger.model.StepFunctionInput;
import com.pearl.downstream.trigger.model.StepFunctionStartResult;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.sfn.SfnClient;
import software.amazon.awssdk.services.sfn.model.ExecutionAlreadyExistsException;
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
                "financial",
                "batch-20260522_prismhr_PEARL-401K-PLAN-001",
                "s3://payroll-outbound-dev/outbound/prismhr/financial/batch-20260522/batch-20260522_prismhr_PEARL-401K-PLAN-001.json",
                "payroll-outbound-dev",
                "outbound/prismhr/financial/batch-20260522/batch-20260522_prismhr_PEARL-401K-PLAN-001.json",
                "ObjectCreated:Put",
                Instant.parse("2026-05-24T10:00:00Z"),
                "ap-south-1",
                4096L,
                "etag-test",
                "sequencer-test",
                "message-1",
                Map.of("source", "unit-test"));

        StepFunctionStartResult result = service.startExecution(input);

        ArgumentCaptor<StartExecutionRequest> captor = ArgumentCaptor.forClass(StartExecutionRequest.class);
        verify(sfnClient).startExecution(captor.capture());
        StartExecutionRequest request = captor.getValue();
        assertEquals("execution-arn", result.executionArn());
        assertEquals(request.name(), result.executionName());
        assertFalse(result.alreadyExists());
        assertEquals("state-machine-arn", request.stateMachineArn());
        assertTrue(request.name().length() <= 80);
        assertFalse(request.name().matches(".*\\d{13}$"));
        assertTrue(request.input().contains("\"correlationId\":\"corr-001\""));
        assertTrue(request.input().contains("\"payloadType\":\"financial\""));
        assertTrue(request.input().contains("\"fileName\":\"batch-20260522_prismhr_PEARL-401K-PLAN-001\""));
        assertTrue(request.input().contains("\"s3PathOrArn\":\"s3://payroll-outbound-dev/outbound/prismhr/financial/batch-20260522/batch-20260522_prismhr_PEARL-401K-PLAN-001.json\""));
        assertTrue(request.input().contains("\"bucket\":\"payroll-outbound-dev\""));
        assertTrue(request.input().contains("\"key\":\"outbound/prismhr/financial/batch-20260522/batch-20260522_prismhr_PEARL-401K-PLAN-001.json\""));
    }

    @Test
    void treatsExistingStepFunctionExecutionAsDuplicateSuccess() {
        SfnClient sfnClient = mock(SfnClient.class);
        when(sfnClient.startExecution(any(StartExecutionRequest.class)))
                .thenThrow(ExecutionAlreadyExistsException.builder().message("execution exists").build());
        StepFunctionStarterService service = new StepFunctionStarterService(sfnClient, "state-machine-arn");

        StepFunctionStartResult result = service.startExecution(input());

        assertTrue(result.alreadyExists());
        assertTrue(result.executionName().length() <= 80);
    }

    private static StepFunctionInput input() {
        return new StepFunctionInput(
                "corr-001",
                "batch-20260522",
                "prismhr",
                "financial",
                "financial",
                "batch-20260522_prismhr_PEARL-401K-PLAN-001",
                "s3://payroll-outbound-dev/outbound/prismhr/financial/batch-20260522/batch-20260522_prismhr_PEARL-401K-PLAN-001.json",
                "payroll-outbound-dev",
                "outbound/prismhr/financial/batch-20260522/batch-20260522_prismhr_PEARL-401K-PLAN-001.json",
                "ObjectCreated:Put",
                Instant.parse("2026-05-24T10:00:00Z"),
                "ap-south-1",
                4096L,
                "etag-test",
                "sequencer-test",
                "message-1",
                Map.of("source", "unit-test", "idempotencyKey", "s3event#test-idempotency-key"));
    }
}
