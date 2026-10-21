package com.roampass.mocksupplier.exception;

import com.roampass.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum MockSupplierErrorCode implements ErrorCode {

    PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND, "모의 공급사에 해당 ICCID가 없습니다."),
    INVALID_STATE(HttpStatus.BAD_REQUEST, "state는 INSTALLED 또는 ENABLED만 됩니다.");

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
