package com.pearl.payroll.common.exception;

public class DownstreamProcessingException extends PearlPayrollException {

    public DownstreamProcessingException(String errorCode, String message) {
        super(errorCode, message);
    }

    public DownstreamProcessingException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
