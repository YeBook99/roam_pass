package com.roampass.admin.dto;

import com.roampass.issue.domain.InstallStatus;
import com.roampass.order.domain.OrderStatus;
import com.roampass.payment.domain.PaymentStatus;
import com.roampass.plan.domain.SimType;
import java.time.LocalDate;

// 주문 목록 필터. 값이 null이면 그 조건은 걸지 않는다
// from·to는 KST 달력 날짜다. 서버에서 UTC 범위로 바꿔 조회한다
public record AdminOrderSearch(OrderStatus orderStatus, PaymentStatus payStatus, InstallStatus installStatus,
                               SimType simType, LocalDate from, LocalDate to) {
}
