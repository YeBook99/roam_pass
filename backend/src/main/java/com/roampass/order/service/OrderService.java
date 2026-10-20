package com.roampass.order.service;

import com.roampass.global.config.ClockConfig;
import com.roampass.global.error.BusinessException;
import com.roampass.history.domain.Actor;
import com.roampass.history.domain.Signal;
import com.roampass.history.domain.TargetType;
import com.roampass.history.service.StatusHistoryService;
import com.roampass.order.domain.OrderNo;
import com.roampass.order.domain.OrderStatus;
import com.roampass.order.domain.SimOrder;
import com.roampass.order.dto.OrderResponse;
import com.roampass.order.dto.PayableOrder;
import com.roampass.order.exception.OrderErrorCode;
import com.roampass.order.repository.SimOrderRepository;
import com.roampass.plan.dto.PlanResponse;
import com.roampass.plan.service.PlanService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final SimOrderRepository orderRepository;
    private final PlanService planService;
    private final StatusHistoryService historyService;
    private final OrderProperties orderProperties;
    private final Clock clock;

    @Transactional
    public OrderResponse create(String userId, Long planId, LocalDate travelStartDate) {
        PlanResponse plan = planService.getPlan(planId);

        // 여행 시작일은 KST 달력 기준이다. UTC로 오늘이어도 KST로는 이미 어제일 수 있다
        LocalDate todayKst = LocalDate.now(clock.withZone(ClockConfig.KST));
        if (travelStartDate.isBefore(todayKst)) {
            throw new BusinessException(OrderErrorCode.PAST_TRAVEL_DATE);
        }

        Instant expiresAt = Instant.now(clock).plus(orderProperties.paymentTtl());
        SimOrder order = orderRepository.save(SimOrder.create(OrderNo.next(), userId, plan.id(), plan.simType(),
                plan.price(), travelStartDate, expiresAt));
        historyService.record(TargetType.ORDER, order.getId(), null, OrderStatus.PENDING.name(),
                Actor.USER, Signal.API, "주문 생성");
        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getMyOrder(String orderNo, String userId) {
        return OrderResponse.from(getMine(orderNo, userId));
    }

    // 결제 기능용: 내 주문인지, 지금 결제할 수 있는지 확인하고 결제에 필요한 값만 돌려준다
    @Transactional(readOnly = true)
    public PayableOrder getPayableOrder(String orderNo, String userId) {
        SimOrder order = getMine(orderNo, userId);
        order.checkPayable(Instant.now(clock));
        return new PayableOrder(order.getId(), order.getOrderNo(), order.getAmount());
    }

    // 결제 기능용: 같은 멱등키 재요청이 내 주문의 것인지 확인할 때 쓴다
    @Transactional(readOnly = true)
    public Long getMyOrderId(String orderNo, String userId) {
        return getMine(orderNo, userId).getId();
    }

    @Transactional(readOnly = true)
    public OrderStatus getStatus(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(OrderErrorCode.ORDER_NOT_FOUND))
                .getStatus();
    }

    // 결제 확정: PENDING→PAID. 이미 PENDING이 아니면(만료 등) false
    @Transactional
    public boolean markPaid(Long orderId, Signal signal) {
        Instant now = Instant.now(clock);
        if (!transit(orderId, OrderStatus.PENDING, OrderStatus.PAID, Actor.PG, signal, "결제 승인", now)) {
            return false;
        }
        orderRepository.recordPaidAt(orderId, now);
        return true;
    }

    // 상태 선점: 허용된 전이인지 검사 → 조건부 UPDATE → 바뀌었을 때만 같은 트랜잭션에서 이력 (CLAUDE.md §3-2)
    private boolean transit(Long orderId, OrderStatus from, OrderStatus to, Actor actor, Signal signal,
                            String reason, Instant now) {
        if (!from.canTransitTo(to)) {
            throw new IllegalStateException("허용되지 않은 주문 전이: " + from + " → " + to);
        }
        if (orderRepository.updateStatusIf(orderId, from.name(), to.name(), now) == 0) {
            return false;   // 다른 요청이 먼저 바꿨다
        }
        historyService.record(TargetType.ORDER, orderId, from.name(), to.name(), actor, signal, reason);
        return true;
    }

    private SimOrder getMine(String orderNo, String userId) {
        // 남의 주문은 "없는 주문"과 똑같이 404로 답한다
        return orderRepository.findByOrderNoAndUserId(orderNo, userId)
                .orElseThrow(() -> new BusinessException(OrderErrorCode.ORDER_NOT_FOUND));
    }
}
