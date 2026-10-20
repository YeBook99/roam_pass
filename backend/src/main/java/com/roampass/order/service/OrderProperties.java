package com.roampass.order.service;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

// application.yml의 app.order.* (ARCHITECTURE.md §14-1)
@ConfigurationProperties("app.order")
public record OrderProperties(Duration paymentTtl) {
}
