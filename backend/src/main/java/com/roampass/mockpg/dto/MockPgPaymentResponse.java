package com.roampass.mockpg.dto;

import com.roampass.mockpg.domain.MockPgPayment;

// 모의 PG가 돌려주는 승인 결과 (승인 응답·웹훅·조회 공통 형식)
public record MockPgPaymentResponse(String merchantRequestId, String pgTid, String orderNo, int amount, String status) {

    public static MockPgPaymentResponse from(MockPgPayment payment) {
        return new MockPgPaymentResponse(payment.getMerchantRequestId(), payment.getPgTid(), payment.getOrderNo(),
                payment.getAmount(), payment.getStatus());
    }

    // 웹훅 금액 조작(시연용)
    public MockPgPaymentResponse withAmountDelta(int delta) {
        return new MockPgPaymentResponse(merchantRequestId, pgTid, orderNo, amount + delta, status);
    }
}
