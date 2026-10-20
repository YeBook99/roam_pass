package com.roampass.payment.service;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

// application.yml의 app.payment.* (ARCHITECTURE.md §14-1)
// reconcileAfter: READY로 이만큼 지난 결제를 PG에 조회한다 / reconcileGiveUpAfter: 이보다 오래된 READY는 더 묻지 않는다
@ConfigurationProperties("app.payment")
public record PaymentProperties(Duration reconcileAfter, Duration reconcileGiveUpAfter) {
}
