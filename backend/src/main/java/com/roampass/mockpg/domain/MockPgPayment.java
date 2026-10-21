package com.roampass.mockpg.domain;

import com.roampass.global.entity.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 모의 PG 쪽 승인 기록 (남의 시스템이라 우리 상태 규칙을 따르지 않는다)
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(schema = "mock", name = "pg_payment")
public class MockPgPayment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String merchantRequestId;
    private String pgTid;
    private String orderNo;
    private int amount;
    private int canceledAmount;
    private String status;

    public static MockPgPayment create(String merchantRequestId, String pgTid, String orderNo, int amount, boolean approved) {
        MockPgPayment payment = new MockPgPayment();
        payment.merchantRequestId = merchantRequestId;
        payment.pgTid = pgTid;
        payment.orderNo = orderNo;
        payment.amount = amount;
        payment.status = approved ? "APPROVED" : "FAILED";
        return payment;
    }

    public boolean isCancelable() {
        return status.equals("APPROVED") || status.equals("PARTIAL_CANCELED");
    }

    public int remaining() {
        return amount - canceledAmount;
    }

    public void cancel(int cancelAmount) {
        canceledAmount += cancelAmount;
        status = canceledAmount == amount ? "CANCELED" : "PARTIAL_CANCELED";
    }
}
