package com.roampass.order.domain;

import com.roampass.plan.domain.SimType;

// 결제가 확정되어 주문이 PAID가 됐다. 커밋 뒤에 발급 기능이 받아서 발급을 시작한다 (ARCHITECTURE.md §6.4)
// 이벤트는 발행하는 기능(order)의 domain에 둔다. 주문 기능은 발급 기능을 모르고, 발급 기능이 이 이벤트를 구독한다
public record OrderPaidEvent(Long orderId, SimType simType) {
}
