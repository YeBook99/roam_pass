package com.roampass.order;

import com.roampass.common.status.Actor;
import com.roampass.common.status.StatusTransitionService;
import com.roampass.common.status.TargetType;
import com.roampass.common.time.Kst;
import com.roampass.common.web.ApiException;
import com.roampass.plan.SimPlan;
import com.roampass.plan.SimPlanRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;

@Service
public class OrderService {

    private final SimOrderRepository orderRepository;
    private final SimPlanRepository planRepository;
    private final StatusTransitionService transition;
    private final Clock clock;
    private final Duration paymentTtl;

    public OrderService(SimOrderRepository orderRepository, SimPlanRepository planRepository,
                        StatusTransitionService transition, Clock clock,
                        @Value("${app.order.payment-ttl}") Duration paymentTtl) {
        this.orderRepository = orderRepository;
        this.planRepository = planRepository;
        this.transition = transition;
        this.clock = clock;
        this.paymentTtl = paymentTtl;
    }

    @Transactional
    public SimOrder create(String userId, Long planId, LocalDate travelStartDate) {
        if (planId == null || travelStartDate == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "planId와 travelStartDate가 필요합니다");
        }
        SimPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PLAN_NOT_FOUND", "요금제가 없습니다"));

        // 여행 시작일은 KST 달력 기준이다. UTC로 오늘이어도 KST로는 이미 어제일 수 있다
        LocalDate todayKst = LocalDate.now(clock.withZone(Kst.ZONE));
        if (travelStartDate.isBefore(todayKst)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PAST_TRAVEL_DATE", "여행 시작일이 지났습니다");
        }

        Instant now = clock.instant();
        SimOrder order = orderRepository.save(SimOrder.create(OrderNo.next(), userId, plan, travelStartDate, now, now.plus(paymentTtl)));
        transition.recordCreated(TargetType.ORDER, order.getId(), OrderStatus.PENDING.name(), Actor.USER, "주문 생성");
        return order;
    }

    @Transactional(readOnly = true)
    public SimOrder getMine(String orderNo, String userId) {
        // 남의 주문은 "없는 주문"과 똑같이 404로 답한다
        return orderRepository.findByOrderNoAndUserId(orderNo, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "주문이 없습니다"));
    }
}
