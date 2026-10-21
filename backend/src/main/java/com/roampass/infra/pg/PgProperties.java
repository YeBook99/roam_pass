package com.roampass.infra.pg;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

// application.yml의 external.pg.*. 비밀키는 ${PG_WEBHOOK_SECRET} 환경 변수로만 받는다 (ARCHITECTURE.md §14)
@ConfigurationProperties("external.pg")
public record PgProperties(String baseUrl, Duration connectTimeout, Duration readTimeout, String webhookSecret) {
}
