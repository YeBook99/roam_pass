package com.roampass.mock.pg;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

// 모의 PG 쪽 승인 기록
@Entity
@Table(schema = "mock", name = "pg_payment")
public class MockPgPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String merchantRequestId;
    private String pgTid;
    private String orderNo;
    private int amount;
    private int canceledAmount;
    private String status;
    private Instant createdAt;

    protected MockPgPayment() {
    }

    MockPgPayment(String merchantRequestId, String pgTid, String orderNo, int amount, String status, Instant createdAt) {
        this.merchantRequestId = merchantRequestId;
        this.pgTid = pgTid;
        this.orderNo = orderNo;
        this.amount = amount;
        this.status = status;
        this.createdAt = createdAt;
    }

    void cancel(int cancelAmount) {
        canceledAmount += cancelAmount;
        status = canceledAmount == amount ? "CANCELED" : "PARTIAL_CANCELED";
    }

    int remaining() {
        return amount - canceledAmount;
    }

    MockPgResult toResult() {
        return new MockPgResult(merchantRequestId, pgTid, orderNo, amount, status);
    }

    String getPgTid() {
        return pgTid;
    }

    int getCanceledAmount() {
        return canceledAmount;
    }

    String getStatus() {
        return status;
    }
}