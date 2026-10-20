package com.roampass.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface SimOrderRepository extends JpaRepository<SimOrder, Long> {

    Optional<SimOrder> findByOrderNoAndUserId(String orderNo, String userId);

    // 상태 선점: 현재 상태가 from일 때만 바꾼다. 반환값 1 = 내가 바꿈, 0 = 이미 다른 요청이 바꿈
    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE sim_order SET status = :to WHERE id = :id AND status = :from", nativeQuery = true)
    int updateStatusIf(@Param("id") long id, @Param("from") String from, @Param("to") String to);

    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE sim_order SET paid_at = :paidAt WHERE id = :id", nativeQuery = true)
    void recordPaidAt(@Param("id") long id, @Param("paidAt") Instant paidAt);
}