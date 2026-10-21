package com.roampass.mocksupplier.domain;

import com.roampass.global.entity.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 모의 공급사가 받은 발급 요청. 접수할 때 ICCID와 활성화 코드를 미리 만들어 둔다
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(schema = "mock", name = "supplier_order")
public class MockSupplierOrder extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String orderNo;
    private String supplierOrderNo;
    private String iccid;
    private String activationCode;

    public static MockSupplierOrder create(String orderNo) {
        MockSupplierOrder order = new MockSupplierOrder();
        order.orderNo = orderNo;
        order.supplierOrderNo = "S-" + UUID.randomUUID();
        order.iccid = "89" + ThreadLocalRandom.current().nextLong(100_000_000_000_000_000L, 999_999_999_999_999_999L);   // 89 + 18자리
        order.activationCode = "LPA:1$smdp.example$" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return order;
    }
}
