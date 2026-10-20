package com.roampass.payment;

import com.roampass.common.status.Actor;
import com.roampass.common.status.Signal;
import com.roampass.common.status.StatusTransitionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.time.Clock;
import java.time.Duration;
import java.util.Optional;

// 대사(reconciliation): 우리 DB의 결제 상태와 PG 기록을 맞춘다 (docs/06 §4)
// 승인 응답이 타임아웃 나고 웹훅도 오지 않아 READY로 남은 결제를 PG에 직접 물어 확정한다
@Component
public class PaymentReconciler {

    private static final Logger log = LoggerFactory.getLogger(PaymentReconciler.class);

    private final PaymentRepository paymentRepository;
    private final PaymentConfirmService confirmService;
    private final StatusTransitionService transition;
    private final PgClient pgClient;
    private final Clock clock;
    private final Duration reconcileAfter;

    public PaymentReconciler(PaymentRepository paymentRepository, PaymentConfirmService confirmService,
                             StatusTransitionService transition, PgClient pgClient, Clock clock,
                             @Value("${app.payment.reconcile-after}") Duration reconcileAfter) {
        this.paymentRepository = paymentRepository;
        this.confirmService = confirmService;
        this.transition = transition;
        this.pgClient = pgClient;
        this.clock = clock;
        this.reconcileAfter = reconcileAfter;
    }

    @Scheduled(fixedDelayString = "${app.payment.reconcile-delay-ms}")
    public void reconcile() {
        // 방금 보낸 요청(아직 응답을 기다리는 중)은 건드리지 않도록 일정 시간이 지난 READY만 본다
        var stuck = paymentRepository.findByStatusAndRequestedAtBefore(PaymentStatus.READY, clock.instant().minus(reconcileAfter));
        for (Payment p : stuck) {
            try {
                Optional<PgClient.Response> found = pgClient.find(p.getId());
                if (found.isPresent()) {
                    confirmService.confirm(found.get().result(), found.get().raw(), Signal.SCHEDULER);
                } else {
                    transition.transitPayment(p.getId(), PaymentStatus.READY, PaymentStatus.FAILED,
                            Actor.SYSTEM, Signal.SCHEDULER, "PG에 승인 기록이 없음");
                }
            } catch (RestClientException e) {
                log.warn("대사 조회 실패, 다음 주기에 다시 시도 paymentId={} : {}", p.getId(), e.getMessage());
            }
        }
    }
}