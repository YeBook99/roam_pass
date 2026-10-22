package com.roampass.payment.service;

import com.roampass.callback.service.CallbackLogService;
import com.roampass.history.domain.Actor;
import com.roampass.history.domain.Signal;
import com.roampass.history.domain.TargetType;
import com.roampass.history.service.StatusHistoryService;
import com.roampass.infra.pg.PgPaymentResult;
import com.roampass.order.service.OrderService;
import com.roampass.payment.domain.Payment;
import com.roampass.payment.domain.PaymentConfirmResult;
import com.roampass.payment.domain.PaymentStatus;
import com.roampass.payment.repository.PaymentRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// PaymentConfirmService 구현. 설명은 인터페이스 PaymentConfirmService에 있다
@Service
@RequiredArgsConstructor
public class PaymentConfirmServiceImpl implements PaymentConfirmService {

    private final PaymentRepository paymentRepository;
    private final CallbackLogService callbackLogService;
    private final OrderService orderService;
    private final StatusHistoryService historyService;
    private final Clock clock;

    // 중복 기록과 상태 변경이 한 트랜잭션이다. 처리 중 실패하면 중복 기록도 같이 롤백되어 다음 재전송이 정상 처리된다
    @Override
    @Transactional
    public PaymentConfirmResult confirm(PgPaymentResult result, Signal signal) {
        // ① 중복 체크: 같은 PG 거래의 같은 결과는 한 번만 기록된다
        String eventKey = "PG:" + result.pgTid() + ":" + result.status();
        if (!callbackLogService.recordIfNew("PG", eventKey, result.rawPayload())) {
            return PaymentConfirmResult.DUPLICATE;
        }

        Optional<Payment> found = findPayment(result.merchantRequestId());
        if (found.isEmpty()) {
            return mark(eventKey, PaymentConfirmResult.UNMATCHED);
        }
        Payment payment = found.get();

        // ② 금액 검증: 통지 금액이 주문 금액(결제 생성 때 복사한 값)과 다르면 결제 완료로 처리하지 않는다
        if (result.amount() != payment.getAmount()) {
            return mark(eventKey, PaymentConfirmResult.AMOUNT_MISMATCH);
        }

        Instant now = Instant.now(clock);
        if (!result.approved()) {
            boolean failed = transit(payment.getId(), PaymentStatus.READY, PaymentStatus.FAILED,
                    Actor.PG, signal, "PG 승인 거절", now);
            return mark(eventKey, failed ? PaymentConfirmResult.PROCESSED : PaymentConfirmResult.STALE_IGNORED);
        }

        // ③ 상태 선점: 결제 READY→APPROVED, 주문 PENDING→PAID. 각각 조건부 UPDATE라 동시에 와도 한 번만 성공한다
        if (!transit(payment.getId(), PaymentStatus.READY, PaymentStatus.APPROVED, Actor.PG, signal, "PG 승인", now)) {
            return mark(eventKey, PaymentConfirmResult.STALE_IGNORED);
        }
        paymentRepository.recordApproval(payment.getId(), result.pgTid(), now);

        if (!orderService.markPaid(payment.getOrderId(), signal)) {
            return mark(eventKey, PaymentConfirmResult.ORDER_NOT_PENDING);
        }
        return mark(eventKey, PaymentConfirmResult.PROCESSED);
    }

    // 대사에서 PG에 승인 기록이 없다고 확인된 결제
    @Override
    @Transactional
    public boolean failWithoutPgRecord(Long paymentId) {
        return transit(paymentId, PaymentStatus.READY, PaymentStatus.FAILED,
                Actor.SYSTEM, Signal.SCHEDULER, "PG에 승인 기록이 없음", Instant.now(clock));
    }

    // 상태 선점: 허용된 전이인지 검사 → 조건부 UPDATE → 바뀌었을 때만 같은 트랜잭션에서 이력 (CLAUDE.md §3-2)
    private boolean transit(Long paymentId, PaymentStatus from, PaymentStatus to, Actor actor, Signal signal,
                            String reason, Instant now) {
        if (!from.canTransitTo(to)) {
            throw new IllegalStateException("허용되지 않은 결제 전이: " + from + " → " + to);
        }
        if (paymentRepository.updateStatusIf(paymentId, from.name(), to.name(), now) == 0) {
            return false;
        }
        historyService.record(TargetType.PAYMENT, paymentId, from.name(), to.name(), actor, signal, reason);
        return true;
    }

    private Optional<Payment> findPayment(String merchantRequestId) {
        try {
            return paymentRepository.findById(Long.parseLong(merchantRequestId));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private PaymentConfirmResult mark(String eventKey, PaymentConfirmResult result) {
        callbackLogService.markResult(eventKey, result.name());
        return result;
    }
}
