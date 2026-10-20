package com.roampass.order;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

// 주문 상태와 허용 전이. docs/04 §2 "허용 전이표"와 1:1로 맞춘다
public enum OrderStatus {
    PENDING, EXPIRED, PAID, ISSUING, ISSUED, PICKUP_READY, ACTIVATING,
    ISSUE_FAILED, REFUNDING, REFUNDED, REFUND_FAILED;

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED = new EnumMap<>(OrderStatus.class);

    static {
        ALLOWED.put(PENDING, EnumSet.of(PAID, EXPIRED));
        ALLOWED.put(PAID, EnumSet.of(ISSUING, PICKUP_READY, REFUNDING));
        ALLOWED.put(ISSUING, EnumSet.of(ISSUING, ISSUED, ISSUE_FAILED));   // ISSUING→ISSUING은 재시도
        ALLOWED.put(PICKUP_READY, EnumSet.of(ACTIVATING, REFUNDING));
        ALLOWED.put(ACTIVATING, EnumSet.of(ISSUED, PICKUP_READY));
        ALLOWED.put(ISSUED, EnumSet.of(REFUNDING));
        ALLOWED.put(ISSUE_FAILED, EnumSet.of(REFUNDING, ISSUING));
        ALLOWED.put(REFUNDING, EnumSet.of(REFUNDED, REFUND_FAILED));
        ALLOWED.put(REFUND_FAILED, EnumSet.of(REFUNDING));
    }

    public boolean canTransitTo(OrderStatus to) {
        return ALLOWED.getOrDefault(this, Set.of()).contains(to);
    }
}
