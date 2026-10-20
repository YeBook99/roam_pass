package com.roampass.payment;

import com.roampass.common.callback.CallbackLogRepository;
import com.roampass.common.status.Actor;
import com.roampass.common.status.Signal;
import com.roampass.common.status.StatusTransitionService;
import com.roampass.order.OrderStatus;
import com.roampass.order.SimOrder;
import com.roampass.order.SimOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

// PG 결과를 우리 DB에 반영하는 유일한 경로. 승인 API 응답·웹훅·대사가 모두 여기로 들어온다 (docs/05 §3)
// 경로가 하나라서 중복 처리 규칙도 하나다
@Service
public class PaymentConfirmService {

    private final CallbackLogRepository callbackLogRepository;
    private final PaymentRepository paymentRepository;
    private final SimOrderRepository orderRepository;
    private final StatusTransitionService transition;
    private final Clock clock;

    public PaymentConfirmService(CallbackLogRepository callbackLogRepository, PaymentRepository paymentRepository,
                                 SimOrderRepository orderRepository, StatusTransitionService transition, Clock clock) {
        this.callbackLogRepository = callbackLogRepository;
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.transition = transition;
        this.clock = clock;
    }

    // 중복 기록과 상태 변경이 한 트랜잭션이다. 처리 중 실패하면 중복 기록도 같이 롤백되어 다음 재전송이 정상 처리된다
    @Transactional
    public ConfirmResult confirm(PgResult r, String rawPayload, Signal signal) {
        Instant now = clock.instant();

        // ① 중복 체크: 같은 PG 거래의 같은 결과는 한 번만 기록된다
        String eventKey = "PG:" + r.pgTid() + ":" + r.status();
        if (callbackLogRepository.insertIgnore("PG", eventKey, rawPayload, now) == 0) {
            return ConfirmResult.DUPLICATE;
        }

        Optional<Payment> found = findPayment(r.merchantRequestId());
        if (found.isEmpty()) {
            return mark(eventKey, ConfirmResult.UNMATCHED);
        }
        Payment payment = found.get();
        SimOrder order = orderRepository.findById(payment.getOrderId()).orElseThrow();

        // ② 금액 검증: 통지 금액이 주문 금액과 다르면 결제 완료로 처리하지 않는다
        if (r.amount() != order.getAmount()) {
            return mark(eventKey, ConfirmResult.AMOUNT_MISMATCH);
        }

        if (!r.approved()) {
            boolean failed = transition.transitPayment(payment.getId(), PaymentStatus.READY, PaymentStatus.FAILED,
                    Actor.PG, signal, "PG 승인 거절");
            return mark(eventKey, failed ? ConfirmResult.PROCESSED : ConfirmResult.STALE_IGNORED);
        }

        // ③ 상태 선점: 결제 READY→APPROVED, 주문 PENDING→PAID. 각각 조건부 UPDATE라 동시에 와도 한 번만 성공한다
        if (!transition.transitPayment(payment.getId(), PaymentStatus.READY, PaymentStatus.APPROVED,
                Actor.PG, signal, "PG 승인")) {
            return mark(eventKey, ConfirmResult.STALE_IGNORED);
        }
        paymentRepository.recordApproval(payment.getId(), r.pgTid(), now);

        if (!transition.transitOrder(order.getId(), OrderStatus.PENDING, OrderStatus.PAID, Actor.PG, signal, "결제 승인")) {
            return mark(eventKey, ConfirmResult.ORDER_NOT_PENDING);
        }
        orderRepository.recordPaidAt(order.getId(), now);
        return mark(eventKey, ConfirmResult.PROCESSED);
    }

    private Optional<Payment> findPayment(String merchantRequestId) {
        try {
            return paymentRepository.findById(Long.parseLong(merchantRequestId));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private ConfirmResult mark(String eventKey, ConfirmResult result) {
        callbackLogRepository.markResult(eventKey, result.name());
        return result;
    }
}