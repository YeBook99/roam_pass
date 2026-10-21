package com.roampass.admin.dto;

import java.time.Instant;

// SQL에 바로 넣을 조건. 상태 값은 문자열(null이면 조건 없음), 기간은 UTC 시각 [createdFrom, createdBefore)
public record AdminOrderFilter(String orderStatus, String payStatus, String installStatus, String simType,
                               Instant createdFrom, Instant createdBefore) {
}
