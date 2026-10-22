package com.roampass.order.service;

import com.roampass.history.domain.Signal;
import com.roampass.order.domain.OrderPaidEvent;
import com.roampass.order.domain.OrderStatus;
import com.roampass.order.dto.OrderResponse;
import com.roampass.order.dto.PayableOrder;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface OrderService {

    OrderResponse create(String userId, Long planId, LocalDate travelStartDate);

    // 내 SIM 목록 화면용. 로그인 시스템이 없어서 X-User-Id로 받은 사용자의 주문만 돌려준다
    List<OrderResponse> getMyOrders(String userId);

    OrderResponse getMyOrder(String orderNo, String userId);

    // 결제 기능용: 내 주문인지, 지금 결제할 수 있는지 확인하고 결제에 필요한 값만 돌려준다
    PayableOrder getPayableOrder(String orderNo, String userId);

    // 결제 기능용: 같은 멱등키 재요청이 내 주문의 것인지 확인할 때 쓴다
    Long getMyOrderId(String orderNo, String userId);

    OrderStatus getStatus(Long orderId);

    // 설치 추적용: 이 주문의 요금제 사용 일수 (만료일 계산에 쓴다)
    int getPlanDays(Long orderId);

    // 결제 확정: PENDING→PAID. 이미 PENDING이 아니면(만료 등) false
    // 성공하면 OrderPaidEvent를 발행한다. 발급 기능이 이 트랜잭션이 커밋된 뒤에 받아서 발급을 시작한다
    boolean markPaid(Long orderId, Signal signal);

    // 발급 기능용 tx1: PAID→ISSUING을 선점하고 요청 시각을 남긴다. 성공하면 주문번호, 다른 요청이 먼저 바꿨으면 empty
    Optional<String> startIssuing(Long orderId, Signal signal);

    // 유심: PAID→PICKUP_READY (공항 수령 대기). 이미 누가 바꿨으면 false
    boolean markPickupReady(Long orderId, Signal signal);

    // 발급 안전망용: 결제된 지 오래됐는데 아직 PAID인 주문 (= 발급이 시작되지 않은 주문)
    List<OrderPaidEvent> getPaidOrdersBefore(Instant before);

    // 발급 기능용 tx2: 공급사가 접수하며 준 supplierOrderNo를 저장한다
    void recordSupplierOrderNo(Long orderId, String supplierOrderNo);

    // 발급 콜백이 어느 주문 것인지 찾는다. 먼저 공급사 접수번호로 찾고,
    // 없으면 콜백에 같이 온 우리 주문번호로 찾는다 (접수번호 저장 전에 서버가 죽었을 수 있다, docs/05 §6)
    Optional<Long> findOrderIdForSupplier(String supplierOrderNo, String orderNo);

    // 발급 완료: ISSUING→ISSUED. 이미 ISSUING이 아니면 false
    boolean markIssued(Long orderId);
}
