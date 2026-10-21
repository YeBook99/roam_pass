package com.roampass.mockpg.repository;

import com.roampass.mockpg.domain.MockPgPayment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MockPgPaymentRepository extends JpaRepository<MockPgPayment, Long> {

    Optional<MockPgPayment> findByMerchantRequestId(String merchantRequestId);

    Optional<MockPgPayment> findByPgTid(String pgTid);
}
