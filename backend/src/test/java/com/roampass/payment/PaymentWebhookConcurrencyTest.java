package com.roampass.payment;

import com.roampass.common.status.Signal;
import com.roampass.common.time.Kst;
import com.roampass.order.OrderService;
import com.roampass.order.SimOrder;
import com.roampass.plan.SimPlanRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

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

import static org.assertj.core.api.Assertions.assertThat;

// 과제 필수 테스트(2.5-5): 같은 결제 통지 N개를 "동시에" 보내도 결제는 한 번만 처리된다
// H2가 아니라 진짜 PostgreSQL에서 돌려야 ON CONFLICT·조건부 UPDATE의 동시성 동작이 그대로 검증된다
@SpringBootTest(properties = {
        "PG_WEBHOOK_SECRET=test-secret",
        "app.payment.reconcile-delay-ms=3600000",   // 테스트 중에 대사 스케줄러가 끼어들지 않게
})
@Testcontainers
class PaymentWebhookConcurrencyTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    OrderService orderService;
    @Autowired
    PaymentTxService paymentTxService;
    @Autowired
    PaymentConfirmService confirmService;
    @Autowired
    SimPlanRepository planRepository;
    @Autowired
    JdbcTemplate jdbc;

    @Test
    void 같은_승인_통지_21개가_동시에_와도_결제는_한_번만_처리된다() throws Exception {
        // 준비: PENDING 주문과 READY 결제 (PG에 승인 요청을 보낸 직후 상태)
        SimOrder order = orderService.create("concurrency-user", planRepository.findAll().get(0).getId(), LocalDate.now(Kst.ZONE));
        Payment payment = paymentTxService.createOrGet(order.getOrderNo(), "concurrency-user", "idem-concurrency").payment();
        PgResult approved = new PgResult(String.valueOf(payment.getId()), "PG-TID-CONCURRENCY", order.getOrderNo(), order.getAmount(), "APPROVED");

        // 실행: 승인 API 응답 1개 + 같은 웹훅 20개를 동시에 출발시킨다
        int threads = 21;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);   // 모든 스레드가 출발선에 설 때까지
        CountDownLatch start = new CountDownLatch(1);         // 출발 신호
        List<Future<ConfirmResult>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            Signal signal = i == 0 ? Signal.API : Signal.WEBHOOK;
            Callable<ConfirmResult> task = () -> {
                ready.countDown();
                start.await();
                return confirmService.confirm(approved, "{\"pgTid\":\"PG-TID-CONCURRENCY\"}", signal);
            };
            futures.add(pool.submit(task));
        }
        ready.await();
        start.countDown();

        Map<ConfirmResult, Integer> results = new EnumMap<>(ConfirmResult.class);
        for (Future<ConfirmResult> f : futures) {
            results.merge(f.get(), 1, Integer::sum);
        }
        pool.shutdown();

        // 검증: 처리 1번, 나머지 20개는 중복. DB에도 승인 1건·전이 이력 1줄·통지 기록 1행만 남는다
        Integer approvedPayments = jdbc.queryForObject(
                "SELECT count(*) FROM payment WHERE order_id = ? AND status = 'APPROVED'", Integer.class, order.getId());
        String orderStatus = jdbc.queryForObject("SELECT status FROM sim_order WHERE id = ?", String.class, order.getId());
        Integer paidHistory = jdbc.queryForObject("""
                SELECT count(*) FROM status_history
                WHERE target_type = 'ORDER' AND target_id = ? AND from_status = 'PENDING' AND to_status = 'PAID'
                """, Integer.class, order.getId());
        Integer callbackRows = jdbc.queryForObject(
                "SELECT count(*) FROM callback_log WHERE event_key = 'PG:PG-TID-CONCURRENCY:APPROVED'", Integer.class);

        System.out.printf("threads=%d, results=%s, payment(APPROVED)=%d, order=%s, history(PENDING→PAID)=%d, callback_log=%d%n",
                threads, results, approvedPayments, orderStatus, paidHistory, callbackRows);

        assertThat(results).containsEntry(ConfirmResult.PROCESSED, 1).containsEntry(ConfirmResult.DUPLICATE, 20);
        assertThat(approvedPayments).isEqualTo(1);
        assertThat(orderStatus).isEqualTo("PAID");
        assertThat(paidHistory).isEqualTo(1);
        assertThat(callbackRows).isEqualTo(1);
    }
}
