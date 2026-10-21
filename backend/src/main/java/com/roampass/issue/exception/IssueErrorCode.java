package com.roampass.issue.exception;

import com.roampass.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum IssueErrorCode implements ErrorCode {

    PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND, "아직 발급된 SIM이 없습니다."),
    INVALID_CALLBACK_SIGNATURE(HttpStatus.UNAUTHORIZED, "공급사 콜백 서명이 올바르지 않습니다."),
    INVALID_CALLBACK_BODY(HttpStatus.BAD_REQUEST, "공급사 콜백 본문을 읽을 수 없습니다.");

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
