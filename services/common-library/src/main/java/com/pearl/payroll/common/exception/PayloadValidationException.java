package com.pearl.payroll.common.exception;

public class PayloadValidationException extends PearlPayrollException {

    public PayloadValidationException(String errorCode, String message) {
        super(errorCode, message);
    }
}
