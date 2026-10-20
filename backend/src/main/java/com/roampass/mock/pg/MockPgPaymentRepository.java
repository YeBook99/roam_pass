package com.roampass.mock.pg;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface MockPgPaymentRepository extends JpaRepository<MockPgPayment, Long> {

    Optional<MockPgPayment> findByMerchantRequestId(String merchantRequestId);

    Optional<MockPgPayment> findByPgTid(String pgTid);
}
