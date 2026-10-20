package com.roampass.common.status;

import com.roampass.order.OrderStatus;
import com.roampass.order.SimOrderRepository;
import com.roampass.payment.PaymentRepository;
import com.roampass.payment.PaymentStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

// 모든 상태 변경이 지나가는 한 곳 (CLAUDE.md §3-2)
// 1) 허용되지 않은 전이는 예외 2) 조건부 UPDATE로 한 번만 바뀜 3) 바뀌었을 때만 같은 트랜잭션에서 이력을 남김
@Service
public class StatusTransitionService {

    private final SimOrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final StatusHistoryRepository historyRepository;
    private final Clock clock;

    public StatusTransitionService(SimOrderRepository orderRepository, PaymentRepository paymentRepository,
                                   StatusHistoryRepository historyRepository, Clock clock) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.historyRepository = historyRepository;
        this.clock = clock;
    }

    @Transactional
    public boolean transitOrder(long orderId, OrderStatus from, OrderStatus to, Actor actor, Signal signal, String reason) {
        if (!from.canTransitTo(to)) {
            throw new IllegalStateException("허용되지 않은 주문 전이: " + from + " → " + to);
        }
        if (orderRepository.updateStatusIf(orderId, from.name(), to.name()) == 0) {
            return false;   // 다른 요청이 먼저 바꿨다
        }
        saveHistory(TargetType.ORDER, orderId, from.name(), to.name(), actor, signal, reason);
        return true;
    }

    @Transactional
    public boolean transitPayment(long paymentId, PaymentStatus from, PaymentStatus to, Actor actor, Signal signal, String reason) {
        if (!from.canTransitTo(to)) {
            throw new IllegalStateException("허용되지 않은 결제 전이: " + from + " → " + to);
        }
        if (paymentRepository.updateStatusIf(paymentId, from.name(), to.name()) == 0) {
            return false;
        }
        saveHistory(TargetType.PAYMENT, paymentId, from.name(), to.name(), actor, signal, reason);
        return true;
    }

    // 생성 이력(null → 첫 상태). 행을 저장한 트랜잭션 안에서만 부른다
    @Transactional(propagation = Propagation.MANDATORY)
    public void recordCreated(TargetType type, long id, String status, Actor actor, String reason) {
        saveHistory(type, id, null, status, actor, Signal.API, reason);
    }

    private void saveHistory(TargetType type, long id, String from, String to, Actor actor, Signal signal, String reason) {
        Instant now = clock.instant();
        historyRepository.save(new StatusHistory(type, id, from, to, actor, signal, reason, now, now));
    }
}
