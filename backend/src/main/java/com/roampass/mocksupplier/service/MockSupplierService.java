package com.roampass.mocksupplier.service;

import com.roampass.mocksupplier.domain.MockSupplierOrder;
import com.roampass.mocksupplier.dto.MockSupplierOrderRequest;
import com.roampass.mocksupplier.dto.MockSupplierOrderResponse;
import com.roampass.mocksupplier.repository.MockSupplierOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 모의 공급사. 시연용이라 mock.enabled=true일 때만 켜진다
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mock.enabled", havingValue = "true")
public class MockSupplierService {

    private final MockSupplierOrderRepository orderRepository;

    // 발급 요청 접수. 같은 주문번호로 다시 오면 처음 만든 접수번호를 그대로 돌려준다 (orderNo가 멱등키)
    @Transactional
    public MockSupplierOrderResponse createOrder(MockSupplierOrderRequest request) {
        MockSupplierOrder order = orderRepository.findByOrderNo(request.orderNo())
                .orElseGet(() -> orderRepository.save(MockSupplierOrder.create(request.orderNo())));
        return new MockSupplierOrderResponse(order.getSupplierOrderNo());
    }
}
