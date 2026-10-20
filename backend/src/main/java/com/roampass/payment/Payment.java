package com.roampass.payment;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

// 결제 시도 한 건. 행은 PaymentRepository.insertIgnore로만 만들고, 상태는 StatusTransitionService로만 바꾼다
@Entity
@Table(name = "payment")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderId;
    private String idempotencyKey;
    private String pgTid;
    private int amount;
    private int canceledAmount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private Instant requestedAt;
    private Instant approvedAt;

    protected Payment() {
    }

    public Long getId() {
        return id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getPgTid() {
        return pgTid;
    }

    public int getAmount() {
        return amount;
    }

    public PaymentStatus getStatus() {
        return status;
    }
}
