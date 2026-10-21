package com.roampass.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

// @Async를 켠다. 결제 확정 이벤트를 받은 발급 요청이 요청 스레드를 붙잡지 않고 별도 스레드에서 돈다 (ADR-05)
// 스레드 풀은 Spring Boot가 자동 등록한 applicationTaskExecutor를 쓴다
@Configuration
@EnableAsync
public class AsyncConfig {
}
