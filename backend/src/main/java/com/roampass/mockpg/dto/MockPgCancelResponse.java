package com.roampass.mockpg.dto;

import com.roampass.mockpg.domain.MockPgPayment;

public record MockPgCancelResponse(String pgTid, String status, int canceledAmount) {

    public static MockPgCancelResponse from(MockPgPayment payment) {
        return new MockPgCancelResponse(payment.getPgTid(), payment.getStatus(), payment.getCanceledAmount());
    }
}
