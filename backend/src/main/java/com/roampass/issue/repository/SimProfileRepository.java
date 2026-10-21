package com.roampass.issue.repository;

import com.roampass.issue.domain.SimProfile;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SimProfileRepository extends JpaRepository<SimProfile, Long> {

    Optional<SimProfile> findByOrderId(Long orderId);

    // 이미 같은 주문·ICCID의 프로필이 있으면 만들지 않고 0을 돌려준다
    @Modifying
    @Query(value = """
            INSERT INTO sim_profile (order_id, iccid, activation_code, supplier_order_no, install_status, created_at, updated_at)
            VALUES (:orderId, :iccid, :activationCode, :supplierOrderNo, 'ISSUED', :now, :now)
            ON CONFLICT DO NOTHING
            """, nativeQuery = true)
    int insertIgnore(@Param("orderId") long orderId, @Param("iccid") String iccid,
                     @Param("activationCode") String activationCode, @Param("supplierOrderNo") String supplierOrderNo,
                     @Param("now") Instant now);
}
