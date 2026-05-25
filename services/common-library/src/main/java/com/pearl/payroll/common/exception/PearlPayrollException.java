package com.pearl.payroll.common.exception;

public class PearlPayrollException extends RuntimeException {

    private final String errorCode;

    public PearlPayrollException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public PearlPayrollException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String errorCode() {
        return errorCode;
    }
}
