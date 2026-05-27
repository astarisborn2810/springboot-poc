package com.pearl.downstream.trigger.idempotency;

import com.pearl.downstream.trigger.model.S3EventMessage;
import com.pearl.downstream.trigger.model.StepFunctionInput;
import com.pearl.downstream.trigger.model.StepFunctionStartResult;

public interface IdempotencyStore extends AutoCloseable {

    IdempotencyClaim claim(S3EventMessage eventMessage, StepFunctionInput input);

    void markStarted(IdempotencyClaim claim, StepFunctionStartResult result);

    void release(IdempotencyClaim claim, Throwable cause);

    @Override
    default void close() {
    }
}
