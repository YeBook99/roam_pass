package com.roampass.mockpg.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

// application.yml의 mock.pg.*. webhookSecret은 원래 PG가 발급하는 키라 우리와 같은 값을 쓴다
// callbackBaseUrl: 모의 PG가 웹훅을 보낼 우리 서버 주소
@ConfigurationProperties("mock.pg")
public record MockPgProperties(String webhookSecret, String callbackBaseUrl) {
}
