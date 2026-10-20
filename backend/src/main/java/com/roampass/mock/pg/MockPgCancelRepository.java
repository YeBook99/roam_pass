package com.roampass.mock.pg;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface MockPgCancelRepository extends JpaRepository<MockPgCancel, Long> {

    Optional<MockPgCancel> findByRequestKey(String requestKey);
}
