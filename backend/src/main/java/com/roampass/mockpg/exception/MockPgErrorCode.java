package com.roampass.mockpg.exception;

import com.roampass.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum MockPgErrorCode implements ErrorCode {

    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "모의 PG에 해당 거래가 없습니다."),
    NOT_CANCELABLE(HttpStatus.CONFLICT, "취소할 수 없는 거래 상태입니다."),
    INVALID_CANCEL_AMOUNT(HttpStatus.BAD_REQUEST, "취소 금액이 올바르지 않습니다."),
    CANCEL_FAILURE(HttpStatus.INTERNAL_SERVER_ERROR, "모의 PG 취소 장애(시연용)입니다.");

    private final HttpStatus status;
    private final String message;

    @Override
    public HttpStatus status() {
        return status;
    }

    @Override
    public String code() {
        return name();
    }

    @Override
    public String message() {
        return message;
    }
}
