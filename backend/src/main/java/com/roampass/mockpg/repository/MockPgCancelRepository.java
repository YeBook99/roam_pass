package com.roampass.mockpg.repository;

import com.roampass.mockpg.domain.MockPgCancel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MockPgCancelRepository extends JpaRepository<MockPgCancel, Long> {

    boolean existsByRequestKey(String requestKey);
}
