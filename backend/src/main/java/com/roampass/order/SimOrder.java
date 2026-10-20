package com.roampass.order;

import com.roampass.plan.SimPlan;
import com.roampass.plan.SimType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

// 주문. 상태(status)를 바꾸는 setter는 두지 않는다. 상태 변경은 StatusTransitionService의 조건부 UPDATE로만 한다
@Entity
@Table(name = "sim_order")
public class SimOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String orderNo;
    private String userId;
    private Long planId;

    @Enumerated(EnumType.STRING)
    private SimType simType;

    private int amount;
    private LocalDate travelStartDate;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private Instant expiresAt;
    private Instant paidAt;
    private Instant createdAt;

    protected SimOrder() {
    }

    public static SimOrder create(String orderNo, String userId, SimPlan plan, LocalDate travelStartDate,
                                  Instant now, Instant expiresAt) {
        SimOrder o = new SimOrder();
        o.orderNo = orderNo;
        o.userId = userId;
        o.planId = plan.getId();
        o.simType = plan.getSimType();
        o.amount = plan.getPrice();   // 주문 시점 가격을 복사한다 (나중에 요금제 가격이 바뀌어도 결제 금액 검증은 이 값으로)
        o.travelStartDate = travelStartDate;
        o.status = OrderStatus.PENDING;
        o.expiresAt = expiresAt;
        o.createdAt = now;
        return o;
    }

    public Long getId() {
        return id;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public String getUserId() {
        return userId;
    }

    public Long getPlanId() {
        return planId;
    }

    public SimType getSimType() {
        return simType;
    }

    public int getAmount() {
        return amount;
    }

    public LocalDate getTravelStartDate() {
        return travelStartDate;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
