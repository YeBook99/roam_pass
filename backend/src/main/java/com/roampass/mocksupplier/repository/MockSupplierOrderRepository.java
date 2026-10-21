package com.roampass.mocksupplier.repository;

import com.roampass.mocksupplier.domain.MockSupplierOrder;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MockSupplierOrderRepository extends JpaRepository<MockSupplierOrder, Long> {

    Optional<MockSupplierOrder> findByOrderNo(String orderNo);
}
