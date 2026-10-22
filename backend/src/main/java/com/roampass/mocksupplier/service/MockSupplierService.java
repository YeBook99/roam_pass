package com.roampass.mocksupplier.service;

import com.roampass.mocksupplier.dto.MockSupplierOrderRequest;
import com.roampass.mocksupplier.dto.MockSupplierOrderResponse;
import com.roampass.mocksupplier.dto.MockSupplierProfileResponse;
import com.roampass.mocksupplier.dto.MockSupplierStateRequest;

// 모의 공급사. 시연용이라 mock.enabled=true일 때만 켜진다
public interface MockSupplierService {

    // 발급 요청 접수. 같은 주문번호로 다시 오면 처음 만든 접수번호를 그대로 돌려준다 (orderNo가 멱등키)
    // 접수하고 몇 초 뒤 결과를 콜백으로 보낸다
    MockSupplierOrderResponse createOrder(MockSupplierOrderRequest request);

    MockSupplierProfileResponse getProfile(String iccid);

    // 시연용: 설치(INSTALLED)·개통(ENABLED)을 흉내 낸다
    MockSupplierProfileResponse changeState(String iccid, MockSupplierStateRequest request);
}
