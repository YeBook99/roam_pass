package com.roampass.payment.service;

import com.roampass.callback.domain.Hmac;
import com.roampass.callback.service.CallbackLogService;
import com.roampass.global.error.BusinessException;
import com.roampass.history.domain.Actor;
import com.roampass.history.domain.Signal;
import com.roampass.history.domain.TargetType;
import com.roampass.history.service.StatusHistoryService;
import com.roampass.infra.pg.PgClient;
import com.roampass.infra.pg.PgPaymentResult;
import com.roampass.infra.pg.PgProperties;
import com.roampass.order.dto.PayableOrder;
import com.roampass.order.service.OrderService;
import com.roampass.payment.domain.Payment;
import com.roampass.payment.domain.PaymentStatus;
import com.roampass.payment.dto.PaymentResponse;
import com.roampass.payment.exception.PaymentErrorCode;
import com.roampass.payment.repository.PaymentRepository;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

// PaymentService 구현. 설명은 인터페이스 PaymentService에 있다
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentConfirmService confirmService;
    private final OrderService orderService;
    private final StatusHistoryService historyService;
    private final CallbackLogService callbackLogService;
    private final PgClient pgClient;
    private final PgProperties pgProperties;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    // Idempotency-Key: 화면이 결제 페이지에 들어올 때 한 번 만든 UUID. 연타·재시도에도 같은 값을 보낸다
    @Override
    public PaymentResponse pay(String orderNo, String userId, String idempotencyKey) {
        // tx1: PG를 부르기 전에 "보내기 직전" 상태(READY)를 먼저 커밋한다 (docs/06 §2)
        Started started = transactionTemplate.execute(status -> start(orderNo, userId, idempotencyKey));

        if (started.created()) {
            // 외부 호출: 트랜잭션 밖. 결과를 모르면(타임아웃·5xx) READY로 두고 웹훅이나 대사 스케줄러가 확정한다
            pgClient.approve(started.paymentId(), orderNo, started.amount())
                    .ifPresent(result -> confirmService.confirm(result, Signal.API));   // tx2: 웹훅·대사와 같은 확정 경로
        }
        // 같은 키의 재요청이면 PG를 다시 부르지 않고 지금 상태를 돌려준다
        return transactionTemplate.execute(status -> toResponse(started.paymentId(), orderNo));
    }

    // 서명을 먼저 검증하고, 검증 전에는 본문을 우리 객체로 바꾸지 않는다 (ARCHITECTURE.md §13-7, CLAUDE.md §3-4)
    // 처리하지 못한 통지도 callback_log에 남긴 뒤 예외를 던진다 (기록은 별도 트랜잭션으로 이미 커밋됨)
    @Override
    public void receiveWebhook(byte[] body, String signature) {
        String raw = new String(body, StandardCharsets.UTF_8);
        if (!Hmac.verify(body, signature, pgProperties.webhookSecret())) {
            callbackLogService.recordRejected("PG", raw, false, "SIGNATURE_INVALID");
            throw new BusinessException(PaymentErrorCode.INVALID_WEBHOOK_SIGNATURE);
        }
        Optional<PgPaymentResult> parsed = pgClient.parseNotification(body);
        if (parsed.isEmpty()) {
            callbackLogService.recordRejected("PG", raw, true, "INVALID_BODY");
            throw new BusinessException(PaymentErrorCode.INVALID_WEBHOOK_BODY);
        }
        confirmService.confirm(parsed.get(), Signal.WEBHOOK);
    }

    // tx1 안에서 실행된다
    private Started start(String orderNo, String userId, String idempotencyKey) {
        // 같은 키의 재요청(버튼 연타·타임아웃 후 재시도)이면 새로 만들지 않고 기존 결제를 돌려준다
        Optional<Payment> existing = paymentRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return Started.existing(sameOrder(existing.get(), orderService.getMyOrderId(orderNo, userId)));
        }

        PayableOrder order = orderService.getPayableOrder(orderNo, userId);
        int inserted = paymentRepository.insertIgnore(order.orderId(), idempotencyKey, order.amount(), Instant.now(clock));
        if (inserted == 0) {
            // 만들지 못했다. 내 키로 된 결제가 있으면 같은 키가 동시에 들어와 다른 요청이 먼저 만든 것이고(그 결과를 돌려준다),
            // 없으면 같은 주문에 다른 키의 READY 결제가 이미 있는 것이다(다른 탭에서 동시에 결제)
            Payment sameKey = paymentRepository.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> new BusinessException(PaymentErrorCode.PAYMENT_IN_PROGRESS));
            return Started.existing(sameOrder(sameKey, order.orderId()));
        }
        Payment payment = paymentRepository.findByIdempotencyKey(idempotencyKey).orElseThrow();
        historyService.record(TargetType.PAYMENT, payment.getId(), null, PaymentStatus.READY.name(),
                Actor.USER, Signal.API, "결제 요청");
        return new Started(payment.getId(), payment.getAmount(), true);
    }

    private PaymentResponse toResponse(Long paymentId, String orderNo) {
        Payment payment = paymentRepository.findById(paymentId).orElseThrow();
        return PaymentResponse.from(payment, orderNo, orderService.getStatus(payment.getOrderId()));
    }

    private static Payment sameOrder(Payment payment, Long orderId) {
        if (!payment.getOrderId().equals(orderId)) {
            throw new BusinessException(PaymentErrorCode.IDEMPOTENCY_KEY_REUSED);
        }
        return payment;
    }

    // tx1의 결과. created=false면 이미 있던 결제라 PG를 부르지 않는다
    private record Started(Long paymentId, int amount, boolean created) {
        static Started existing(Payment payment) {
            return new Started(payment.getId(), payment.getAmount(), false);
        }
    }
}
