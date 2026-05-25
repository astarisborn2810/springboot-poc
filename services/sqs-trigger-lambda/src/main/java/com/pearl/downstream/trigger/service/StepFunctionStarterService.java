package com.pearl.downstream.trigger.service;

import com.pearl.downstream.trigger.exception.ConfigurationException;
import com.pearl.downstream.trigger.exception.TriggerProcessingException;
import com.pearl.downstream.trigger.model.StepFunctionInput;
import com.pearl.downstream.trigger.util.JsonUtil;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Function;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sfn.SfnClient;
import software.amazon.awssdk.services.sfn.model.SfnException;
import software.amazon.awssdk.services.sfn.model.StartExecutionRequest;
import software.amazon.awssdk.services.sfn.model.StartExecutionResponse;

public class StepFunctionStarterService implements AutoCloseable {

    private static final int MAX_EXECUTION_NAME_LENGTH = 80;

    private final SfnClient sfnClient;
    private final String stateMachineArn;
    private final boolean closeClient;

    public StepFunctionStarterService() {
        this(System::getenv);
    }

    StepFunctionStarterService(Function<String, String> environmentProvider) {
        this(createClient(environmentProvider), environmentProvider.apply("STATE_MACHINE_ARN"), true);
    }

    public StepFunctionStarterService(SfnClient sfnClient, String stateMachineArn) {
        this(sfnClient, stateMachineArn, false);
    }

    private StepFunctionStarterService(SfnClient sfnClient, String stateMachineArn, boolean closeClient) {
        this.sfnClient = sfnClient;
        this.stateMachineArn = requireStateMachineArn(stateMachineArn);
        this.closeClient = closeClient;
    }

    public String startExecution(StepFunctionInput input) {
        StartExecutionRequest request = StartExecutionRequest.builder()
                .stateMachineArn(stateMachineArn)
                .name(executionName(input))
                .input(JsonUtil.toJson(input))
                .build();
        try {
            StartExecutionResponse response = sfnClient.startExecution(request);
            return response.executionArn();
        } catch (SfnException ex) {
            throw new TriggerProcessingException("Failed to start Step Function execution", ex);
        }
    }

    @Override
    public void close() {
        if (closeClient) {
            sfnClient.close();
        }
    }

    private static SfnClient createClient(Function<String, String> environmentProvider) {
        String region = environmentProvider.apply("AWS_REGION");
        if (region != null && !region.isBlank()) {
            return SfnClient.builder().region(Region.of(region)).build();
        }
        return SfnClient.builder().build();
    }

    private static String requireStateMachineArn(String stateMachineArn) {
        if (stateMachineArn == null || stateMachineArn.isBlank()) {
            throw new ConfigurationException("STATE_MACHINE_ARN environment variable is required");
        }
        return stateMachineArn;
    }

    private static String executionName(StepFunctionInput input) {
        String rawName = String.join("-",
                nullToUnknown(input.vendorId()),
                nullToUnknown(input.dataType()),
                nullToUnknown(input.batchId()),
                input.correlationId(),
                String.valueOf(Instant.now().toEpochMilli()));
        String sanitized = rawName.replaceAll("[^A-Za-z0-9_-]", "-");
        if (sanitized.length() <= MAX_EXECUTION_NAME_LENGTH) {
            return sanitized;
        }
        String suffix = "-" + Optional.ofNullable(input.correlationId())
                .map(value -> value.replaceAll("[^A-Za-z0-9_-]", ""))
                .filter(value -> value.length() >= 12)
                .map(value -> value.substring(0, 12))
                .orElse("correlation");
        int prefixLength = Math.max(1, MAX_EXECUTION_NAME_LENGTH - suffix.length());
        return sanitized.substring(0, prefixLength) + suffix;
    }

    private static String nullToUnknown(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }
}
