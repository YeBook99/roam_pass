package com.roampass.payment;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

// 결제 상태. READY = PG에 요청을 보냈지만 결과를 아직 모름(타임아웃이면 여기서 멈춤). docs/04 §3
public enum PaymentStatus {
    READY, APPROVED, FAILED, PARTIAL_CANCELED, CANCELED;

    private static final Map<PaymentStatus, Set<PaymentStatus>> ALLOWED = new EnumMap<>(PaymentStatus.class);

    static {
        ALLOWED.put(READY, EnumSet.of(APPROVED, FAILED));
        ALLOWED.put(APPROVED, EnumSet.of(PARTIAL_CANCELED, CANCELED));
        ALLOWED.put(PARTIAL_CANCELED, EnumSet.of(PARTIAL_CANCELED, CANCELED));
    }

    public boolean canTransitTo(PaymentStatus to) {
        return ALLOWED.getOrDefault(this, Set.of()).contains(to);
    }
}
