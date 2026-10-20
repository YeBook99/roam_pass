package com.roampass.global.error;

import org.springframework.http.HttpStatus;

// 기능별 enum이 이 인터페이스를 구현한다 (ARCHITECTURE.md §10.1)
public interface ErrorCode {

    HttpStatus status();

    String code();

    String message();
}
