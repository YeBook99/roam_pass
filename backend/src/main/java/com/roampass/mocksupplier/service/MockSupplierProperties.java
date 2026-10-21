package com.roampass.mocksupplier.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

// application.yml의 mock.supplier.*
// callbackSecret: 원래는 공급사와 나눠 가진 키라 우리와 같은 값을 쓴다 / callbackBaseUrl: 콜백을 보낼 우리 서버 주소
// callbackDelayMs: 접수 후 콜백을 보내기까지 기다리는 시간
@ConfigurationProperties("mock.supplier")
public record MockSupplierProperties(String callbackSecret, String callbackBaseUrl, long callbackDelayMs) {
}
