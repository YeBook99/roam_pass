package com.roampass.order.service;

import com.roampass.global.config.ClockConfig;
import com.roampass.global.error.BusinessException;
import com.roampass.history.domain.Actor;
import com.roampass.history.domain.Signal;
import com.roampass.history.domain.TargetType;
import com.roampass.history.service.StatusHistoryService;
import com.roampass.order.domain.OrderNo;
import com.roampass.order.domain.OrderPaidEvent;
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
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final SimOrderRepository orderRepository;
    private final PlanService planService;
    private final StatusHistoryService historyService;
    private final OrderProperties orderProperties;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Override
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

    // 내 SIM 목록 화면용. 로그인 시스템이 없어서 X-User-Id로 받은 사용자의 주문만 돌려준다
    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(String userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(OrderResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getMyOrder(String orderNo, String userId) {
        return OrderResponse.from(getMine(orderNo, userId));
    }

    // 결제 기능용: 내 주문인지, 지금 결제할 수 있는지 확인하고 결제에 필요한 값만 돌려준다
    @Override
    @Transactional(readOnly = true)
    public PayableOrder getPayableOrder(String orderNo, String userId) {
        SimOrder order = getMine(orderNo, userId);
        order.checkPayable(Instant.now(clock));
        return new PayableOrder(order.getId(), order.getOrderNo(), order.getAmount());
    }

    // 결제 기능용: 같은 멱등키 재요청이 내 주문의 것인지 확인할 때 쓴다
    @Override
    @Transactional(readOnly = true)
    public Long getMyOrderId(String orderNo, String userId) {
        return getMine(orderNo, userId).getId();
    }

    @Override
    @Transactional(readOnly = true)
    public OrderStatus getStatus(Long orderId) {
        return getOrder(orderId).getStatus();
    }

    // 설치 추적용: 이 주문의 요금제 사용 일수 (만료일 계산에 쓴다)
    @Override
    @Transactional(readOnly = true)
    public int getPlanDays(Long orderId) {
        return planService.getPlan(getOrder(orderId).getPlanId()).days();
    }

    // 결제 확정: PENDING→PAID. 이미 PENDING이 아니면(만료 등) false
    // 성공하면 OrderPaidEvent를 발행한다. 발급 기능이 이 트랜잭션이 커밋된 뒤에 받아서 발급을 시작한다
    @Override
    @Transactional
    public boolean markPaid(Long orderId, Signal signal) {
        Instant now = Instant.now(clock);
        if (!transit(orderId, OrderStatus.PENDING, OrderStatus.PAID, Actor.PG, signal, "결제 승인", now)) {
            return false;
        }
        orderRepository.recordPaidAt(orderId, now);
        eventPublisher.publishEvent(new OrderPaidEvent(orderId, getOrder(orderId).getSimType()));
        return true;
    }

    // 발급 기능용 tx1: PAID→ISSUING을 선점하고 요청 시각을 남긴다. 성공하면 주문번호, 다른 요청이 먼저 바꿨으면 empty
    @Override
    @Transactional
    public Optional<String> startIssuing(Long orderId, Signal signal) {
        Instant now = Instant.now(clock);
        if (!transit(orderId, OrderStatus.PAID, OrderStatus.ISSUING, Actor.SYSTEM, signal, "발급 요청", now)) {
            return Optional.empty();
        }
        orderRepository.recordIssueRequestedAt(orderId, now);
        return Optional.of(getOrder(orderId).getOrderNo());
    }

    // 유심: PAID→PICKUP_READY (공항 수령 대기). 이미 누가 바꿨으면 false
    @Override
    @Transactional
    public boolean markPickupReady(Long orderId, Signal signal) {
        return transit(orderId, OrderStatus.PAID, OrderStatus.PICKUP_READY, Actor.SYSTEM, signal,
                "유심 수령 대기", Instant.now(clock));
    }

    // 발급 안전망용: 결제된 지 오래됐는데 아직 PAID인 주문 (= 발급이 시작되지 않은 주문)
    @Override
    @Transactional(readOnly = true)
    public List<OrderPaidEvent> getPaidOrdersBefore(Instant before) {
        return orderRepository.findByStatusAndPaidAtBefore(OrderStatus.PAID, before).stream()
                .map(order -> new OrderPaidEvent(order.getId(), order.getSimType()))
                .toList();
    }

    // 발급 기능용 tx2: 공급사가 접수하며 준 supplierOrderNo를 저장한다
    @Override
    @Transactional
    public void recordSupplierOrderNo(Long orderId, String supplierOrderNo) {
        if (orderRepository.recordSupplierOrderNo(orderId, supplierOrderNo, Instant.now(clock)) == 0) {
            log.warn("이미 다른 공급사 접수번호가 있는 주문 orderId={}, supplierOrderNo={}", orderId, supplierOrderNo);
        }
    }

    // 발급 콜백이 어느 주문 것인지 찾는다. 먼저 공급사 접수번호로 찾고,
    // 없으면 콜백에 같이 온 우리 주문번호로 찾는다 (접수번호 저장 전에 서버가 죽었을 수 있다, docs/05 §6)
    @Override
    @Transactional
    public Optional<Long> findOrderIdForSupplier(String supplierOrderNo, String orderNo) {
        Optional<SimOrder> bySupplier = orderRepository.findBySupplierOrderNo(supplierOrderNo);
        if (bySupplier.isPresent()) {
            return Optional.of(bySupplier.get().getId());
        }
        Optional<SimOrder> byOrderNo = orderRepository.findByOrderNo(orderNo);
        byOrderNo.ifPresent(order -> recordSupplierOrderNo(order.getId(), supplierOrderNo));
        return byOrderNo.map(SimOrder::getId);
    }

    // 발급 완료: ISSUING→ISSUED. 이미 ISSUING이 아니면 false
    @Override
    @Transactional
    public boolean markIssued(Long orderId) {
        return transit(orderId, OrderStatus.ISSUING, OrderStatus.ISSUED, Actor.SUPPLIER, Signal.CALLBACK,
                "발급 완료", Instant.now(clock));
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

    // clearAutomatically 조건부 UPDATE 뒤에 부르면 DB의 최신 상태를 다시 읽는다
    private SimOrder getOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(OrderErrorCode.ORDER_NOT_FOUND));
    }

    private SimOrder getMine(String orderNo, String userId) {
        // 남의 주문은 "없는 주문"과 똑같이 404로 답한다
        return orderRepository.findByOrderNoAndUserId(orderNo, userId)
                .orElseThrow(() -> new BusinessException(OrderErrorCode.ORDER_NOT_FOUND));
    }
}
