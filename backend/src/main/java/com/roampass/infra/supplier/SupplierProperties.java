package com.roampass.infra.supplier;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

// application.yml의 external.supplier.* (ARCHITECTURE.md §14)
@ConfigurationProperties("external.supplier")
public record SupplierProperties(String baseUrl, Duration connectTimeout, Duration readTimeout) {
}