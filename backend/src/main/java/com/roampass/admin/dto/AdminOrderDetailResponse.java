package com.roampass.admin.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

// 주문 상세. 주문·최신 결제·프로필 정보와 시간순 타임라인
// fetchedAt = 이 데이터를 읽은 시각. 화면이 "N초 전 기준"을 보여 줄 때 쓴다
public record AdminOrderDetailResponse(Order order, List<TimelineItem> timeline, Instant fetchedAt) {

    // 결제·프로필 쪽 값은 아직 없으면 null
    public record Order(String orderNo, String userId, String simType, int amount, LocalDate travelStartDate,
                        String orderStatus, Instant createdAt, Instant paidAt, String supplierOrderNo,
                        String payStatus, String pgTid, Instant approvedAt,
                        String iccid, String installStatus, Instant qrViewedAt, Instant installedAt,
                        Instant activatedAt, Instant expiresAt) {
    }
}
