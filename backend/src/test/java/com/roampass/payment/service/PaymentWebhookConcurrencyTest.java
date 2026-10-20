package com.roampass.payment.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.roampass.history.domain.Signal;
import com.roampass.global.config.ClockConfig;
import com.roampass.infra.pg.PgPaymentResult;
import com.roampass.order.dto.OrderResponse;
import com.roampass.order.service.OrderService;
import com.roampass.payment.domain.PaymentConfirmResult;
import com.roampass.payment.domain.PaymentStatus;
import com.roampass.payment.dto.PaymentResponse;
import com.roampass.plan.service.PlanService;
import com.roampass.support.IntegrationTestSupport;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

// 과제 필수 테스트(2.5-5): 같은 결제 통지 N개를 "동시에" 보내도 결제는 한 번만 처리된다
// H2가 아니라 진짜 PostgreSQL에서 돌려야 ON CONFLICT·조건부 UPDATE의 동시성 동작이 그대로 검증된다
// 동시성 테스트라 @Transactional을 붙이지 않는다(커밋돼야 다른 스레드가 본다, ARCHITECTURE.md §16-3)
class PaymentWebhookConcurrencyTest extends IntegrationTestSupport {

    @Autowired
    PlanService planService;
    @Autowired
    OrderService orderService;
    @Autowired
    PaymentService paymentService;
    @Autowired
    PaymentConfirmService confirmService;
    @Autowired
    JdbcTemplate jdbc;

    @Test
    @DisplayName("같은 승인 통지 21개가 동시에 와도 결제는 한 번만 처리된다")
    void 같은_승인_통지_21개가_동시에_와도_결제는_한_번만_처리된다() throws Exception {
        // 준비: PENDING 주문과 READY 결제 (PG 주소가 닿지 않는 곳이라 승인 결과를 "모름" → READY로 남는다)
        String user = "concurrency-user";
        OrderResponse order = orderService.create(user, planService.getPlans().get(0).id(), LocalDate.now(ClockConfig.KST));
        PaymentResponse payment = paymentService.pay(order.orderNo(), user, "idem-concurrency");
        assertThat(payment.paymentStatus()).isEqualTo(PaymentStatus.READY);

        PgPaymentResult approved = new PgPaymentResult(String.valueOf(payment.paymentId()), "PG-TID-CONCURRENCY",
                order.orderNo(), order.amount(), "APPROVED", "{\"pgTid\":\"PG-TID-CONCURRENCY\"}");

        // 실행: 승인 API 응답 1개 + 같은 웹훅 20개를 동시에 출발시킨다
        int threads = 21;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);   // 모든 스레드가 출발선에 설 때까지
        CountDownLatch start = new CountDownLatch(1);         // 출발 신호
        List<Future<PaymentConfirmResult>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            Signal signal = i == 0 ? Signal.API : Signal.WEBHOOK;
            Callable<PaymentConfirmResult> task = () -> {
                ready.countDown();
                start.await();
                return confirmService.confirm(approved, signal);
            };
            futures.add(pool.submit(task));
        }
        ready.await();
        start.countDown();

        Map<PaymentConfirmResult, Integer> results = new EnumMap<>(PaymentConfirmResult.class);
        for (Future<PaymentConfirmResult> f : futures) {
            results.merge(f.get(), 1, Integer::sum);
        }
        pool.shutdown();

        // 검증: 처리 1번, 나머지 20개는 중복. DB에도 승인 1건·전이 이력 1줄·통지 기록 1행만 남는다
        Long orderId = jdbc.queryForObject("SELECT id FROM sim_order WHERE order_no = ?", Long.class, order.orderNo());
        Integer approvedPayments = jdbc.queryForObject(
                "SELECT count(*) FROM payment WHERE order_id = ? AND status = 'APPROVED'", Integer.class, orderId);
        String orderStatus = jdbc.queryForObject("SELECT status FROM sim_order WHERE id = ?", String.class, orderId);
        Integer paidHistory = jdbc.queryForObject("""
                SELECT count(*) FROM status_history
                WHERE target_type = 'ORDER' AND target_id = ? AND from_status = 'PENDING' AND to_status = 'PAID'
                """, Integer.class, orderId);
        Integer callbackRows = jdbc.queryForObject(
                "SELECT count(*) FROM callback_log WHERE event_key = 'PG:PG-TID-CONCURRENCY:APPROVED'", Integer.class);

        System.out.printf("threads=%d, results=%s, payment(APPROVED)=%d, order=%s, history(PENDING→PAID)=%d, callback_log=%d%n",
                threads, results, approvedPayments, orderStatus, paidHistory, callbackRows);

        assertThat(results).containsEntry(PaymentConfirmResult.PROCESSED, 1).containsEntry(PaymentConfirmResult.DUPLICATE, 20);
        assertThat(approvedPayments).isEqualTo(1);
        assertThat(orderStatus).isEqualTo("PAID");
        assertThat(paidHistory).isEqualTo(1);
        assertThat(callbackRows).isEqualTo(1);
    }
}
