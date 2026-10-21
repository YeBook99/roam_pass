package com.roampass.issue.service;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

// application.yml의 app.issue.*
// paidPickupAfter: 결제 후 이만큼 지나도 PAID로 남은 주문을 안전망 스케줄러가 다시 처리한다
@ConfigurationProperties("app.issue")
public record IssueProperties(Duration paidPickupAfter) {
}
