package com.roampass.payment.repository;

import com.roampass.payment.domain.Payment;
import com.roampass.payment.domain.PaymentStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    List<Payment> findByStatusAndRequestedAtBetween(PaymentStatus status, Instant from, Instant to);

    // 만들 수 없으면 예외 대신 0을 돌려준다. 걸리는 UNIQUE가 둘이라 충돌 대상을 정하지 않는다
    //  - 같은 Idempotency-Key가 이미 있음 (버튼 연타·재시도)
    //  - 같은 주문에 READY 결제가 이미 있음 (부분 유니크 ux_payment_order_ready, 다른 탭 동시 결제)
    // 대상을 (idempotency_key)로만 정하면, 같은 키 요청 두 개가 동시에 올 때 ux_payment_order_ready에 먼저 걸려
    // 예외가 났다(같은 키인데 409, 2026-10-21 동시 요청 재현). 0이면 누가 먼저 만들었는지는 호출한 쪽이 키로 다시 찾아 판단한다
    @Modifying
    @Query(value = """
            INSERT INTO payment (order_id, idempotency_key, amount, status, requested_at, created_at, updated_at)
            VALUES (:orderId, :key, :amount, 'READY', :now, :now, :now)
            ON CONFLICT DO NOTHING
            """, nativeQuery = true)
    int insertIgnore(@Param("orderId") long orderId, @Param("key") String key,
                     @Param("amount") int amount, @Param("now") Instant now);

    // 상태 선점: 현재 상태가 from일 때만 바꾼다. 반환값 1 = 내가 바꿈, 0 = 이미 다른 요청이 바꿈
    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE payment SET status = :to, updated_at = :now WHERE id = :id AND status = :from",
            nativeQuery = true)
    int updateStatusIf(@Param("id") long id, @Param("from") String from, @Param("to") String to,
                       @Param("now") Instant now);

    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE payment SET pg_tid = :pgTid, approved_at = :now, updated_at = :now WHERE id = :id",
            nativeQuery = true)
    int recordApproval(@Param("id") long id, @Param("pgTid") String pgTid, @Param("now") Instant now);
}
