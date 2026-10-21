package com.roampass.payment.exception;

import com.roampass.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum PaymentErrorCode implements ErrorCode {

    PAYMENT_IN_PROGRESS(HttpStatus.CONFLICT, "이 주문은 다른 결제가 진행 중입니다."),
    IDEMPOTENCY_KEY_REUSED(HttpStatus.CONFLICT, "다른 주문에 쓴 Idempotency-Key입니다."),
    INVALID_WEBHOOK_SIGNATURE(HttpStatus.UNAUTHORIZED, "웹훅 서명이 올바르지 않습니다."),
    INVALID_WEBHOOK_BODY(HttpStatus.BAD_REQUEST, "웹훅 본문을 읽을 수 없습니다.");

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
