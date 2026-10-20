package com.roampass.global.error;

import lombok.Getter;

// 업무 규칙 위반. GlobalExceptionHandler가 ErrorCode로 응답을 만든다 (ARCHITECTURE.md §10)
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.message());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String detail) {
        super(detail);
        this.errorCode = errorCode;
    }
}
