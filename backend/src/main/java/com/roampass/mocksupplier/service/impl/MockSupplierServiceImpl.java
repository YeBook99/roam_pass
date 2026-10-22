package com.roampass.mocksupplier.service.impl;

import com.roampass.global.error.BusinessException;
import com.roampass.mocksupplier.domain.MockSupplierOrder;
import com.roampass.mocksupplier.dto.MockSupplierOrderRequest;
import com.roampass.mocksupplier.dto.MockSupplierOrderResponse;
import com.roampass.mocksupplier.dto.MockSupplierProfileResponse;
import com.roampass.mocksupplier.dto.MockSupplierStateRequest;
import com.roampass.mocksupplier.exception.MockSupplierErrorCode;
import com.roampass.mocksupplier.repository.MockSupplierOrderRepository;
import com.roampass.mocksupplier.service.MockSupplierCallbackSender;
import com.roampass.mocksupplier.service.MockSupplierService;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// MockSupplierService 구현. 설명은 인터페이스 MockSupplierService에 있다
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mock.enabled", havingValue = "true")
public class MockSupplierServiceImpl implements MockSupplierService {

    private final MockSupplierOrderRepository orderRepository;
    private final MockSupplierCallbackSender callbackSender;
    private final Clock clock;

    // 발급 요청 접수. 같은 주문번호로 다시 오면 처음 만든 접수번호를 그대로 돌려준다 (orderNo가 멱등키)
    // 접수하고 몇 초 뒤 결과를 콜백으로 보낸다
    @Override
    @Transactional
    public MockSupplierOrderResponse createOrder(MockSupplierOrderRequest request) {
        MockSupplierOrder order = orderRepository.findByOrderNo(request.orderNo())
                .orElseGet(() -> orderRepository.save(MockSupplierOrder.create(request.orderNo())));
        callbackSender.schedule(order);
        return new MockSupplierOrderResponse(order.getSupplierOrderNo());
    }

    @Override
    @Transactional(readOnly = true)
    public MockSupplierProfileResponse getProfile(String iccid) {
        return MockSupplierProfileResponse.from(getOrder(iccid));
    }

    // 시연용: 설치(INSTALLED)·개통(ENABLED)을 흉내 낸다
    @Override
    @Transactional
    public MockSupplierProfileResponse changeState(String iccid, MockSupplierStateRequest request) {
        if (!"INSTALLED".equals(request.state()) && !"ENABLED".equals(request.state())) {
            throw new BusinessException(MockSupplierErrorCode.INVALID_STATE);
        }
        MockSupplierOrder order = getOrder(iccid);
        Instant activatedAt = request.activatedAt() != null ? request.activatedAt() : Instant.now(clock);
        order.changeState(request.state(), activatedAt);
        return MockSupplierProfileResponse.from(order);
    }

    private MockSupplierOrder getOrder(String iccid) {
        return orderRepository.findByIccid(iccid)
                .orElseThrow(() -> new BusinessException(MockSupplierErrorCode.PROFILE_NOT_FOUND));
    }
}
