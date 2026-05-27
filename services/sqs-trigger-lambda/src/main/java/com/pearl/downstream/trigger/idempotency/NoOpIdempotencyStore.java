package com.pearl.downstream.trigger.idempotency;

import com.pearl.downstream.trigger.model.S3EventMessage;
import com.pearl.downstream.trigger.model.StepFunctionInput;
import com.pearl.downstream.trigger.model.StepFunctionStartResult;
import com.pearl.downstream.trigger.util.IdempotencyKeyUtil;

public class NoOpIdempotencyStore implements IdempotencyStore {

    @Override
    public IdempotencyClaim claim(S3EventMessage eventMessage, StepFunctionInput input) {
        return IdempotencyClaim.acquired(IdempotencyKeyUtil.forInput(input));
    }

    @Override
    public void markStarted(IdempotencyClaim claim, StepFunctionStartResult result) {
    }

    @Override
    public void release(IdempotencyClaim claim, Throwable cause) {
    }
}
