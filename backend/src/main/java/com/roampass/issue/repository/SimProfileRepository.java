package com.roampass.issue.repository;

import com.roampass.issue.domain.InstallStatus;
import com.roampass.issue.domain.SimProfile;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SimProfileRepository extends JpaRepository<SimProfile, Long> {

    Optional<SimProfile> findByOrderId(Long orderId);

    List<SimProfile> findByInstallStatusIn(Collection<InstallStatus> statuses);

    List<SimProfile> findByInstallStatusAndExpiresAtBefore(InstallStatus status, Instant before);

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

    // 상태 선점: 현재 설치 상태가 from일 때만 바꾼다. 반환값 1 = 내가 바꿈, 0 = 이미 다른 요청이 바꿈
    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE sim_profile SET install_status = :to, updated_at = :now WHERE id = :id AND install_status = :from",
            nativeQuery = true)
    int updateInstallStatusIf(@Param("id") long id, @Param("from") String from, @Param("to") String to,
                              @Param("now") Instant now);

    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE sim_profile SET qr_viewed_at = :now, updated_at = :now WHERE id = :id", nativeQuery = true)
    int recordQrViewedAt(@Param("id") long id, @Param("now") Instant now);

    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE sim_profile SET installed_at = :now, updated_at = :now WHERE id = :id", nativeQuery = true)
    int recordInstalledAt(@Param("id") long id, @Param("now") Instant now);

    @Modifying(clearAutomatically = true)
    @Query(value = """
            UPDATE sim_profile SET activated_at = :activatedAt, expires_at = :expiresAt, updated_at = :now WHERE id = :id
            """, nativeQuery = true)
    int recordActivation(@Param("id") long id, @Param("activatedAt") Instant activatedAt,
                         @Param("expiresAt") Instant expiresAt, @Param("now") Instant now);
}
