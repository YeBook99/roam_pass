package com.roampass.mocksupplier.dto;

import com.roampass.mocksupplier.domain.MockSupplierOrder;
import java.time.Instant;

// 모의 공급사의 프로필 상태 조회 응답
public record MockSupplierProfileResponse(String iccid, String state, Instant activatedAt) {

    public static MockSupplierProfileResponse from(MockSupplierOrder order) {
        return new MockSupplierProfileResponse(order.getIccid(), order.getState(), order.getActivatedAt());
    }
}
