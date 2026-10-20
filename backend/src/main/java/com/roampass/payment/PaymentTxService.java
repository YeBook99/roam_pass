package com.roampass.payment;

import com.roampass.common.status.Actor;
import com.roampass.common.status.StatusTransitionService;
import com.roampass.common.status.TargetType;
import com.roampass.common.web.ApiException;
import com.roampass.order.OrderStatus;
import com.roampass.order.SimOrder;
import com.roampass.order.SimOrderRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

// 결제 흐름의 tx1: PG를 부르기 전에 "보내기 직전" 상태(READY)를 먼저 커밋한다 (docs/06 §2)
// PaymentFacade와 다른 클래스로 둔 이유: 같은 클래스 안에서 부르면 @Transactional이 걸리지 않는다
@Service
public class PaymentTxService {

    public record Started(Payment payment, boolean created) {
    }

    private final SimOrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final StatusTransitionService transition;
    private final Clock clock;

    public PaymentTxService(SimOrderRepository orderRepository, PaymentRepository paymentRepository,
                            StatusTransitionService transition, Clock clock) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.transition = transition;
        this.clock = clock;
    }

    @Transactional
    public Started createOrGet(String orderNo, String userId, String idempotencyKey) {
        SimOrder order = orderRepository.findByOrderNoAndUserId(orderNo, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "주문이 없습니다"));

        // 같은 키의 재요청(버튼 연타·타임아웃 후 재시도)이면 새로 만들지 않고 기존 결제를 돌려준다
        Optional<Payment> existing = paymentRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return new Started(sameOrder(existing.get(), order), false);
        }

        Instant now = clock.instant();
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, "ORDER_NOT_PAYABLE", "결제할 수 없는 주문 상태입니다: " + order.getStatus());
        }
        if (!order.getExpiresAt().isAfter(now)) {
            throw new ApiException(HttpStatus.CONFLICT, "ORDER_EXPIRED", "결제 대기 시간이 지났습니다");
        }

        int inserted;
        try {
            inserted = paymentRepository.insertIgnore(order.getId(), idempotencyKey, order.getAmount(), now);
        } catch (DataIntegrityViolationException e) {
            // 같은 주문에 READY 결제가 이미 있음 (다른 탭에서 동시에 결제)
            throw new ApiException(HttpStatus.CONFLICT, "PAYMENT_IN_PROGRESS", "이 주문은 다른 결제가 진행 중입니다");
        }
        Payment payment = paymentRepository.findByIdempotencyKey(idempotencyKey).orElseThrow();
        if (inserted == 0) {
            return new Started(sameOrder(payment, order), false);   // 같은 키가 동시에 들어와 다른 요청이 먼저 만들었다
        }
        transition.recordCreated(TargetType.PAYMENT, payment.getId(), PaymentStatus.READY.name(), Actor.USER, "결제 요청");
        return new Started(payment, true);
    }

    private static Payment sameOrder(Payment payment, SimOrder order) {
        if (!payment.getOrderId().equals(order.getId())) {
            throw new ApiException(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_REUSED", "다른 주문에 쓴 Idempotency-Key입니다");
        }
        return payment;
    }
}