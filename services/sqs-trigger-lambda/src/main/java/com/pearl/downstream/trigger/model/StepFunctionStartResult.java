package com.pearl.downstream.trigger.model;

public record StepFunctionStartResult(
        String executionName,
        String executionArn,
        boolean alreadyExists) {

    public static StepFunctionStartResult started(String executionName, String executionArn) {
        return new StepFunctionStartResult(executionName, executionArn, false);
    }

    public static StepFunctionStartResult alreadyExists(String executionName) {
        return new StepFunctionStartResult(executionName, null, true);
    }
}
