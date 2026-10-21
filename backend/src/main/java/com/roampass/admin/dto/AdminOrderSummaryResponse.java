package com.roampass.admin.dto;

import java.time.Instant;

// 주문 목록 한 줄. 결제는 그 주문의 최신 결제, 설치 상태는 프로필이 없으면 null
public record AdminOrderSummaryResponse(String orderNo, String userId, String simType, int amount,
                                        String orderStatus, String payStatus, String installStatus,
                                        Instant createdAt, Instant paidAt) {
}
