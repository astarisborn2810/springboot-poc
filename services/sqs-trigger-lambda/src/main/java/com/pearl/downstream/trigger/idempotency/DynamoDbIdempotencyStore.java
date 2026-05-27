package com.pearl.downstream.trigger.idempotency;

import com.pearl.downstream.trigger.exception.TriggerProcessingException;
import com.pearl.downstream.trigger.model.CorrelationMetadata;
import com.pearl.downstream.trigger.model.S3EventMessage;
import com.pearl.downstream.trigger.model.StepFunctionInput;
import com.pearl.downstream.trigger.model.StepFunctionStartResult;
import com.pearl.downstream.trigger.util.IdempotencyKeyUtil;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

public class DynamoDbIdempotencyStore implements IdempotencyStore {

    private static final String IDEMPOTENCY_TABLE_NAME = "IDEMPOTENCY_TABLE_NAME";
    private static final String IDEMPOTENCY_TTL_DAYS = "IDEMPOTENCY_TTL_DAYS";
    private static final String IDEMPOTENCY_IN_PROGRESS_TTL_SECONDS = "IDEMPOTENCY_IN_PROGRESS_TTL_SECONDS";

    private static final long DEFAULT_TTL_DAYS = 91;
    private static final long DEFAULT_IN_PROGRESS_TTL_SECONDS = 900;

    private static final String STATUS_STARTING = "STARTING";
    private static final String STATUS_STARTED = "STARTED";
    private static final String STATUS_FAILED = "FAILED";

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;
    private final long ttlDays;
    private final long inProgressTtlSeconds;
    private final boolean closeClient;

    public static IdempotencyStore fromEnvironment(Function<String, String> environmentProvider) {
        String tableName = environmentProvider.apply(IDEMPOTENCY_TABLE_NAME);
        if (tableName == null || tableName.isBlank()) {
            return new NoOpIdempotencyStore();
        }
        return new DynamoDbIdempotencyStore(
                createClient(environmentProvider),
                tableName,
                positiveLong(environmentProvider.apply(IDEMPOTENCY_TTL_DAYS), DEFAULT_TTL_DAYS),
                positiveLong(environmentProvider.apply(IDEMPOTENCY_IN_PROGRESS_TTL_SECONDS),
                        DEFAULT_IN_PROGRESS_TTL_SECONDS),
                true);
    }

    public DynamoDbIdempotencyStore(
            DynamoDbClient dynamoDbClient,
            String tableName,
            long ttlDays,
            long inProgressTtlSeconds) {
        this(dynamoDbClient, tableName, ttlDays, inProgressTtlSeconds, false);
    }

    private DynamoDbIdempotencyStore(
            DynamoDbClient dynamoDbClient,
            String tableName,
            long ttlDays,
            long inProgressTtlSeconds,
            boolean closeClient) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
        this.ttlDays = ttlDays;
        this.inProgressTtlSeconds = inProgressTtlSeconds;
        this.closeClient = closeClient;
    }

    @Override
    public IdempotencyClaim claim(S3EventMessage eventMessage, StepFunctionInput input) {
        String idempotencyKey = IdempotencyKeyUtil.forInput(input);
        long now = Instant.now().getEpochSecond();
        long leaseExpiresAt = now + inProgressTtlSeconds;
        try {
            dynamoDbClient.putItem(PutItemRequest.builder()
                    .tableName(tableName)
                    .item(itemFor(eventMessage, input, idempotencyKey, now, leaseExpiresAt))
                    .conditionExpression("attribute_not_exists(#idempotencyKey)"
                            + " OR #status = :failed"
                            + " OR (#status = :starting AND #leaseExpiresAt < :now)")
                    .expressionAttributeNames(Map.of(
                            "#idempotencyKey", "idempotencyKey",
                            "#status", "status",
                            "#leaseExpiresAt", "leaseExpiresAt"))
                    .expressionAttributeValues(Map.of(
                            ":failed", s(STATUS_FAILED),
                            ":starting", s(STATUS_STARTING),
                            ":now", n(now)))
                    .build());
            return IdempotencyClaim.acquired(idempotencyKey);
        } catch (ConditionalCheckFailedException ex) {
            return existingClaim(idempotencyKey);
        } catch (DynamoDbException ex) {
            throw new TriggerProcessingException("Unable to claim S3 idempotency record", ex);
        }
    }

    @Override
    public void markStarted(IdempotencyClaim claim, StepFunctionStartResult result) {
        if (claim == null || !claim.acquired()) {
            return;
        }
        long now = Instant.now().getEpochSecond();
        Map<String, AttributeValue> values = new LinkedHashMap<>();
        values.put(":started", s(STATUS_STARTED));
        values.put(":updatedAt", n(now));
        values.put(":executionName", s(result.executionName()));
        values.put(":alreadyExists", bool(result.alreadyExists()));
        Map<String, String> names = new LinkedHashMap<>();
        names.put("#status", "status");
        names.put("#updatedAt", "updatedAt");
        names.put("#executionName", "executionName");
        names.put("#alreadyExists", "alreadyExists");
        names.put("#failureMessage", "failureMessage");
        names.put("#leaseExpiresAt", "leaseExpiresAt");
        if (result.executionArn() != null && !result.executionArn().isBlank()) {
            values.put(":executionArn", s(result.executionArn()));
            names.put("#executionArn", "executionArn");
        }

        String updateExpression = result.executionArn() == null || result.executionArn().isBlank()
                ? "SET #status = :started, #updatedAt = :updatedAt, #executionName = :executionName,"
                        + " #alreadyExists = :alreadyExists REMOVE #failureMessage, #leaseExpiresAt"
                : "SET #status = :started, #updatedAt = :updatedAt, #executionName = :executionName,"
                        + " #executionArn = :executionArn, #alreadyExists = :alreadyExists"
                        + " REMOVE #failureMessage, #leaseExpiresAt";

        try {
            dynamoDbClient.updateItem(UpdateItemRequest.builder()
                    .tableName(tableName)
                    .key(key(claim.idempotencyKey()))
                    .updateExpression(updateExpression)
                    .expressionAttributeNames(names)
                    .expressionAttributeValues(values)
                    .build());
        } catch (DynamoDbException ex) {
            throw new TriggerProcessingException("Unable to mark S3 idempotency record as started", ex);
        }
    }

    @Override
    public void release(IdempotencyClaim claim, Throwable cause) {
        if (claim == null || !claim.acquired()) {
            return;
        }
        long now = Instant.now().getEpochSecond();
        try {
            dynamoDbClient.updateItem(UpdateItemRequest.builder()
                    .tableName(tableName)
                    .key(key(claim.idempotencyKey()))
                    .updateExpression("SET #status = :failed, #updatedAt = :updatedAt,"
                            + " #failureMessage = :failureMessage REMOVE #leaseExpiresAt")
                    .expressionAttributeNames(Map.of(
                            "#status", "status",
                            "#updatedAt", "updatedAt",
                            "#failureMessage", "failureMessage",
                            "#leaseExpiresAt", "leaseExpiresAt"))
                    .expressionAttributeValues(Map.of(
                            ":failed", s(STATUS_FAILED),
                            ":updatedAt", n(now),
                            ":failureMessage", s(failureMessage(cause))))
                    .build());
        } catch (DynamoDbException ex) {
            throw new TriggerProcessingException("Unable to release S3 idempotency claim", ex);
        }
    }

    @Override
    public void close() {
        if (closeClient) {
            dynamoDbClient.close();
        }
    }

    private IdempotencyClaim existingClaim(String idempotencyKey) {
        try {
            GetItemResponse response = dynamoDbClient.getItem(GetItemRequest.builder()
                    .tableName(tableName)
                    .key(key(idempotencyKey))
                    .consistentRead(true)
                    .build());
            String status = stringValue(response.item(), "status");
            if (STATUS_STARTED.equals(status)) {
                return IdempotencyClaim.duplicateCompleted(idempotencyKey, status);
            }
            return IdempotencyClaim.inProgress(idempotencyKey, status);
        } catch (DynamoDbException ex) {
            throw new TriggerProcessingException("Unable to read existing S3 idempotency record", ex);
        }
    }

    private Map<String, AttributeValue> itemFor(
            S3EventMessage eventMessage,
            StepFunctionInput input,
            String idempotencyKey,
            long now,
            long leaseExpiresAt) {
        Map<String, AttributeValue> item = new LinkedHashMap<>();
        item.put("idempotencyKey", s(idempotencyKey));
        item.put("status", s(STATUS_STARTING));
        item.put("createdAt", n(now));
        item.put("updatedAt", n(now));
        item.put("leaseExpiresAt", n(leaseExpiresAt));
        item.put("expiresAt", n(now + ttlDays * 24 * 60 * 60));
        putIfPresent(item, "bucket", eventMessage.bucketName());
        putIfPresent(item, "objectKey", eventMessage.objectKey());
        putIfPresent(item, "eventType", eventMessage.eventType());
        putIfPresent(item, "eTag", eventMessage.eTag());
        putIfPresent(item, "sequencer", eventMessage.sequencer());
        putIfPresent(item, "sourceMessageId", eventMessage.sourceMessageId());
        putIfPresent(item, "fileName", input.fileName());
        putIfPresent(item, "s3PathOrArn", input.s3PathOrArn());

        CorrelationMetadata correlation = eventMessage.correlation();
        if (correlation != null) {
            putIfPresent(item, "correlationId", correlation.correlationId());
            putIfPresent(item, "batchId", correlation.batchId());
            putIfPresent(item, "vendorId", correlation.vendorId());
            putIfPresent(item, "dataType", correlation.dataType());
        }
        return item;
    }

    private static DynamoDbClient createClient(Function<String, String> environmentProvider) {
        String region = environmentProvider.apply("AWS_REGION");
        if (region != null && !region.isBlank()) {
            return DynamoDbClient.builder().region(Region.of(region)).build();
        }
        return DynamoDbClient.builder().build();
    }

    private static Map<String, AttributeValue> key(String idempotencyKey) {
        return Map.of("idempotencyKey", s(idempotencyKey));
    }

    private static AttributeValue s(String value) {
        return AttributeValue.builder().s(value).build();
    }

    private static AttributeValue n(long value) {
        return AttributeValue.builder().n(String.valueOf(value)).build();
    }

    private static AttributeValue bool(boolean value) {
        return AttributeValue.builder().bool(value).build();
    }

    private static void putIfPresent(Map<String, AttributeValue> item, String key, String value) {
        if (value != null && !value.isBlank()) {
            item.put(key, s(value));
        }
    }

    private static String stringValue(Map<String, AttributeValue> item, String key) {
        if (item == null || item.isEmpty() || item.get(key) == null) {
            return null;
        }
        return item.get(key).s();
    }

    private static String failureMessage(Throwable cause) {
        if (cause == null) {
            return "unknown";
        }
        String message = cause.getMessage();
        if (message == null || message.isBlank()) {
            message = cause.getClass().getName();
        }
        return message.length() > 512 ? message.substring(0, 512) : message;
    }

    private static long positiveLong(String value, long fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            long parsed = Long.parseLong(value);
            return parsed > 0 ? parsed : fallback;
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }
}
