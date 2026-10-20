package com.roampass.payment.service;

import com.roampass.global.error.BusinessException;
import com.roampass.history.domain.Signal;
import com.roampass.infra.pg.PgClient;
import com.roampass.infra.pg.PgPaymentResult;
import com.roampass.payment.domain.Payment;
import com.roampass.payment.domain.PaymentStatus;
import com.roampass.payment.repository.PaymentRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 대사(reconciliation): 우리 DB의 결제 상태와 PG 기록을 맞춘다 (docs/06 §4)
// 승인 응답이 타임아웃 나고 웹훅도 오지 않아 READY로 남은 결제를 PG에 직접 물어 확정한다
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentReconcileScheduler {

    private final PaymentRepository paymentRepository;
    private final PaymentConfirmService confirmService;
    private final PgClient pgClient;
    private final PaymentProperties paymentProperties;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${app.payment.reconcile-delay-ms}")
    public void reconcile() {
        // 방금 보낸 요청(아직 응답 대기 중)은 건드리지 않고, 너무 오래된 것은 더 묻지 않는다(무한 재조회 방지, CLAUDE.md §3-6)
        // 15분이 지나도 READY인 결제는 사람이 볼 대상이다 (어드민 이상 건 PAYMENT_UNKNOWN, docs/09)
        Instant now = Instant.now(clock);
        List<Payment> stuck = paymentRepository.findByStatusAndRequestedAtBetween(PaymentStatus.READY,
                now.minus(paymentProperties.reconcileGiveUpAfter()), now.minus(paymentProperties.reconcileAfter()));
        for (Payment payment : stuck) {
            try {
                Optional<PgPaymentResult> found = pgClient.find(payment.getId());
                if (found.isPresent()) {
                    confirmService.confirm(found.get(), Signal.SCHEDULER);
                } else {
                    confirmService.failWithoutPgRecord(payment.getId());
                }
            } catch (BusinessException e) {
                log.warn("대사 조회 실패, 다음 주기에 다시 시도 paymentId={} : {}", payment.getId(), e.getMessage());
            } catch (RuntimeException e) {
                // 예상하지 못한 예외(DB 제약 위반 등)도 이 건만 건너뛰고 다음 결제를 처리한다. 한 건 때문에 뒤 건이 밀리지 않게
                log.error("대사 처리 중 예상하지 못한 오류 paymentId={}", payment.getId(), e);
            }
        }
    }
}
