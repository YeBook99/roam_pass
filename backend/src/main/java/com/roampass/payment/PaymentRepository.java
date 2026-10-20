package com.roampass.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    List<Payment> findByStatusAndRequestedAtBefore(PaymentStatus status, Instant before);

    // 같은 Idempotency-Key가 이미 있으면 만들지 않고 0을 돌려준다 (버튼 연타·재시도)
    // 같은 주문에 READY가 이미 있으면 부분 유니크(ux_payment_order_ready) 위반 예외가 난다 (다른 탭 동시 결제)
    @Modifying
    @Query(value = """
            INSERT INTO payment (order_id, idempotency_key, amount, status, requested_at)
            VALUES (:orderId, :key, :amount, 'READY', :requestedAt)
            ON CONFLICT (idempotency_key) DO NOTHING
            """, nativeQuery = true)
    int insertIgnore(@Param("orderId") long orderId, @Param("key") String key,
                     @Param("amount") int amount, @Param("requestedAt") Instant requestedAt);

    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE payment SET status = :to WHERE id = :id AND status = :from", nativeQuery = true)
    int updateStatusIf(@Param("id") long id, @Param("from") String from, @Param("to") String to);

    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE payment SET pg_tid = :pgTid, approved_at = :approvedAt WHERE id = :id", nativeQuery = true)
    void recordApproval(@Param("id") long id, @Param("pgTid") String pgTid, @Param("approvedAt") Instant approvedAt);
}
