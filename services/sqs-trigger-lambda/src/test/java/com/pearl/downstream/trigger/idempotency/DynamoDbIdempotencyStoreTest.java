package com.pearl.downstream.trigger.idempotency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pearl.downstream.trigger.model.CorrelationMetadata;
import com.pearl.downstream.trigger.model.S3EventMessage;
import com.pearl.downstream.trigger.model.StepFunctionInput;
import com.pearl.downstream.trigger.model.StepFunctionStartResult;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;

class DynamoDbIdempotencyStoreTest {

    @Test
    void claimsNewS3EventWithConditionalPutAndLease() {
        DynamoDbClient client = mock(DynamoDbClient.class);
        when(client.putItem(any(PutItemRequest.class))).thenReturn(PutItemResponse.builder().build());
        DynamoDbIdempotencyStore store = new DynamoDbIdempotencyStore(client, "idempotency-table", 91, 900);

        IdempotencyClaim claim = store.claim(eventMessage(), input());

        ArgumentCaptor<PutItemRequest> captor = ArgumentCaptor.forClass(PutItemRequest.class);
        verify(client).putItem(captor.capture());
        PutItemRequest request = captor.getValue();
        assertTrue(claim.acquired());
        assertEquals("idempotency-table", request.tableName());
        assertTrue(request.conditionExpression().contains("attribute_not_exists"));
        assertEquals("STARTING", request.item().get("status").s());
        assertTrue(request.item().containsKey("leaseExpiresAt"));
        assertTrue(request.item().containsKey("expiresAt"));
    }

    @Test
    void classifiesStartedRecordAsDuplicateCompleted() {
        DynamoDbClient client = mock(DynamoDbClient.class);
        when(client.putItem(any(PutItemRequest.class)))
                .thenThrow(ConditionalCheckFailedException.builder().message("duplicate").build());
        when(client.getItem(any(GetItemRequest.class))).thenReturn(GetItemResponse.builder()
                .item(Map.of("status", AttributeValue.builder().s("STARTED").build()))
                .build());
        DynamoDbIdempotencyStore store = new DynamoDbIdempotencyStore(client, "idempotency-table", 91, 900);

        IdempotencyClaim claim = store.claim(eventMessage(), input());

        assertTrue(claim.duplicateCompleted());
        assertEquals("STARTED", claim.existingStatus());
    }

    @Test
    void classifiesStartingRecordAsInProgress() {
        DynamoDbClient client = mock(DynamoDbClient.class);
        when(client.putItem(any(PutItemRequest.class)))
                .thenThrow(ConditionalCheckFailedException.builder().message("duplicate").build());
        when(client.getItem(any(GetItemRequest.class))).thenReturn(GetItemResponse.builder()
                .item(Map.of("status", AttributeValue.builder().s("STARTING").build()))
                .build());
        DynamoDbIdempotencyStore store = new DynamoDbIdempotencyStore(client, "idempotency-table", 91, 900);

        IdempotencyClaim claim = store.claim(eventMessage(), input());

        assertTrue(claim.inProgress());
        assertEquals("STARTING", claim.existingStatus());
    }

    @Test
    void marksAcquiredClaimAsStartedAfterStepFunctionStarts() {
        DynamoDbClient client = mock(DynamoDbClient.class);
        when(client.updateItem(any(UpdateItemRequest.class))).thenReturn(UpdateItemResponse.builder().build());
        DynamoDbIdempotencyStore store = new DynamoDbIdempotencyStore(client, "idempotency-table", 91, 900);
        IdempotencyClaim claim = IdempotencyClaim.acquired("s3event#abc");

        store.markStarted(claim, StepFunctionStartResult.started("execution-name", "execution-arn"));

        ArgumentCaptor<UpdateItemRequest> captor = ArgumentCaptor.forClass(UpdateItemRequest.class);
        verify(client).updateItem(captor.capture());
        UpdateItemRequest request = captor.getValue();
        assertTrue(request.updateExpression().contains("#status = :started"));
        assertEquals("STARTED", request.expressionAttributeValues().get(":started").s());
        assertEquals("execution-arn", request.expressionAttributeValues().get(":executionArn").s());
    }

    private static S3EventMessage eventMessage() {
        return new S3EventMessage(
                "payroll-outbound-dev",
                "outbound/prismhr/financial/batch-20260522/batch-20260522_prismhr_PEARL-401K-PLAN-001.json",
                "ObjectCreated:Put",
                Instant.parse("2026-05-24T10:00:00Z"),
                "ap-south-1",
                4096L,
                "etag-test",
                "sequencer-test",
                "message-1",
                "{}",
                new CorrelationMetadata("corr-001", "batch-20260522", "prismhr", "financial"));
    }

    private static StepFunctionInput input() {
        return StepFunctionInput.from(eventMessage());
    }
}
