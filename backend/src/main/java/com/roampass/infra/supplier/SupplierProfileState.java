package com.roampass.infra.supplier;

import java.time.Instant;

// 공급사가 알려 준 프로필 상태. state = RELEASED(아직 안 내려받음) | INSTALLED | ENABLED(개통)
// activatedAt은 공급사 기준 UTC 개통 시각(개통 전이면 null)
public record SupplierProfileState(String state, Instant activatedAt) {
}
