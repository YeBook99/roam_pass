package com.roampass.payment;

import com.roampass.common.status.Signal;
import com.roampass.common.web.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

// 결제 요청의 순서를 조율한다. 이 클래스에는 트랜잭션이 없다 (CLAUDE.md §3-1)
// tx1(READY 커밋) → PG HTTP 호출(트랜잭션 밖) → tx2(결과 반영)
@Service
public class PaymentFacade {

    private static final Logger log = LoggerFactory.getLogger(PaymentFacade.class);

    private final PaymentTxService paymentTxService;
    private final PaymentConfirmService confirmService;
    private final PaymentRepository paymentRepository;
    private final PgClient pgClient;

    public PaymentFacade(PaymentTxService paymentTxService, PaymentConfirmService confirmService,
                         PaymentRepository paymentRepository, PgClient pgClient) {
        this.paymentTxService = paymentTxService;
        this.confirmService = confirmService;
        this.paymentRepository = paymentRepository;
        this.pgClient = pgClient;
    }

    public Payment pay(String orderNo, String userId, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "MISSING_IDEMPOTENCY_KEY", "Idempotency-Key 헤더가 필요합니다");
        }

        PaymentTxService.Started started = paymentTxService.createOrGet(orderNo, userId, idempotencyKey);
        if (!started.created()) {
            return started.payment();   // 같은 키의 재요청: PG를 다시 부르지 않고 지금 상태를 돌려준다
        }

        Payment payment = started.payment();
        try {
            PgClient.Response res = pgClient.approve(payment.getId(), orderNo, payment.getAmount());
            confirmService.confirm(res.result(), res.raw(), Signal.API);
        } catch (RestClientException e) {
            // 타임아웃·5xx는 "실패"가 아니라 "모름"이다. 돈이 이미 빠져나갔을 수 있으므로 READY로 두고
            // 웹훅이나 대사 스케줄러가 확정하게 한다 (docs/06 §4)
            log.warn("PG 승인 결과를 모름 paymentId={} : {}", payment.getId(), e.getMessage());
        }
        return paymentRepository.findById(payment.getId()).orElseThrow();
    }
}