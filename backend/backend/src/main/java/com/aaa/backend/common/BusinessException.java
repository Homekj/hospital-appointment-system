package com.aaa.backend.common;

public class BusinessException extends RuntimeException {
    private final BusinessErrorCode errorCode;

    public BusinessException(BusinessErrorCode errorCode) {
        super(errorCode.defaultMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(BusinessErrorCode errorCode, String message) {
        super(message == null || message.isBlank() ? errorCode.defaultMessage() : message);
        this.errorCode = errorCode;
    }

    public BusinessErrorCode getErrorCode() {
        return errorCode;
    }
}
