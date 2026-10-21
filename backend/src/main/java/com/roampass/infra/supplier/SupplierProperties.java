package com.roampass.infra.supplier;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

// application.yml의 external.supplier.*. 콜백 서명 키는 ${SUPPLIER_CALLBACK_SECRET} 환경 변수로만 받는다
@ConfigurationProperties("external.supplier")
public record SupplierProperties(String baseUrl, Duration connectTimeout, Duration readTimeout, String callbackSecret) {
}
