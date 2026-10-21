package com.roampass.order.domain;

import com.roampass.global.entity.BaseTimeEntity;
import com.roampass.global.error.BusinessException;
import com.roampass.order.exception.OrderErrorCode;
import com.roampass.plan.domain.SimType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 주문. 상태(status)는 엔티티 메서드로 바꾸지 않고 OrderService의 조건부 UPDATE로만 바꾼다 (CLAUDE.md §3-2)
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SimOrder extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String orderNo;
    private String userId;
    private Long planId;          // 다른 기능(plan)의 Entity는 ID로 참조한다 (§4-8)

    @Enumerated(EnumType.STRING)
    private SimType simType;

    private int amount;           // 주문 시점 가격을 복사한다. 나중에 요금제 가격이 바뀌어도 결제 금액 검증은 이 값으로
    private LocalDate travelStartDate;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private Instant expiresAt;
    private Instant paidAt;
    private String supplierOrderNo;   // 공급사 접수번호. 발급 요청이 접수되면 채워진다

    public static SimOrder create(String orderNo, String userId, Long planId, SimType simType, int amount,
                                  LocalDate travelStartDate, Instant expiresAt) {
        SimOrder order = new SimOrder();
        order.orderNo = orderNo;
        order.userId = userId;
        order.planId = planId;
        order.simType = simType;
        order.amount = amount;
        order.travelStartDate = travelStartDate;
        order.status = OrderStatus.PENDING;
        order.expiresAt = expiresAt;
        return order;
    }

    // 결제를 시작할 수 있는지: 결제 대기 상태이고 만료 전이어야 한다
    public void checkPayable(Instant now) {
        if (status != OrderStatus.PENDING) {
            throw new BusinessException(OrderErrorCode.ORDER_NOT_PAYABLE, "결제할 수 없는 주문 상태입니다: " + status);
        }
        if (!expiresAt.isAfter(now)) {
            throw new BusinessException(OrderErrorCode.ORDER_EXPIRED);
        }
    }
}
