package com.pearl.downstream.trigger.exception;

public class TriggerProcessingException extends RuntimeException {

    public TriggerProcessingException(String message) {
        super(message);
    }

    public TriggerProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
