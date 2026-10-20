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

    // 같은 Idempotency-Key가 이미 있으면 만들지 않고 0을 돌려준다 (버튼 연타·재시도)
    // 같은 주문에 READY가 이미 있으면 부분 유니크(ux_payment_order_ready) 위반 예외가 난다 (다른 탭 동시 결제)
    @Modifying
    @Query(value = """
            INSERT INTO payment (order_id, idempotency_key, amount, status, requested_at, created_at, updated_at)
            VALUES (:orderId, :key, :amount, 'READY', :now, :now, :now)
            ON CONFLICT (idempotency_key) DO NOTHING
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
