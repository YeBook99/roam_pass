package com.roampass.order.repository;

import com.roampass.order.domain.OrderStatus;
import com.roampass.order.domain.SimOrder;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SimOrderRepository extends JpaRepository<SimOrder, Long> {

    Optional<SimOrder> findByOrderNoAndUserId(String orderNo, String userId);

    Optional<SimOrder> findBySupplierOrderNo(String supplierOrderNo);

    Optional<SimOrder> findByOrderNo(String orderNo);

    List<SimOrder> findByStatusAndPaidAtBefore(OrderStatus status, Instant before);

    // 상태 선점: 현재 상태가 from일 때만 바꾼다. 반환값 1 = 내가 바꿈, 0 = 이미 다른 요청이 바꿈
    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE sim_order SET status = :to, updated_at = :now WHERE id = :id AND status = :from",
            nativeQuery = true)
    int updateStatusIf(@Param("id") long id, @Param("from") String from, @Param("to") String to,
                       @Param("now") Instant now);

    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE sim_order SET paid_at = :paidAt, updated_at = :paidAt WHERE id = :id", nativeQuery = true)
    int recordPaidAt(@Param("id") long id, @Param("paidAt") Instant paidAt);

    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE sim_order SET issue_requested_at = :now, updated_at = :now WHERE id = :id", nativeQuery = true)
    int recordIssueRequestedAt(@Param("id") long id, @Param("now") Instant now);

    @Modifying(clearAutomatically = true)
    // 이미 다른 접수번호가 들어 있으면 덮어쓰지 않는다 (반환값 0)
    @Query(value = """
            UPDATE sim_order SET supplier_order_no = :supplierOrderNo, updated_at = :now
            WHERE id = :id AND (supplier_order_no IS NULL OR supplier_order_no = :supplierOrderNo)
            """, nativeQuery = true)
    int recordSupplierOrderNo(@Param("id") long id, @Param("supplierOrderNo") String supplierOrderNo,
                              @Param("now") Instant now);
}
