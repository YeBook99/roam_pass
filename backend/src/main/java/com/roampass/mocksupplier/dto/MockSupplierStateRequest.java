package com.roampass.mocksupplier.dto;

import java.time.Instant;

// 시연용 상태 변경. state = INSTALLED | ENABLED
// activatedAt은 ENABLED일 때 개통 시각. 비우면 지금 시각 (과제 예시 시각을 넣어 만료일 계산을 보여 줄 수 있다)
public record MockSupplierStateRequest(String state, Instant activatedAt) {
}
