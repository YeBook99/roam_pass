package com.roampass.mocksupplier.controller;

import com.roampass.mocksupplier.dto.MockSupplierOrderRequest;
import com.roampass.mocksupplier.dto.MockSupplierOrderResponse;
import com.roampass.mocksupplier.service.MockSupplierService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// 모의 공급사 API. 시연용이라 mock.enabled=true일 때만 켜진다
@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mock.enabled", havingValue = "true")
public class MockSupplierController {

    private final MockSupplierService mockSupplierService;

    // 접수만 바로 응답한다(202 Accepted). 결과(ICCID)는 나중에 콜백으로 보낸다
    @PostMapping("/mock/supplier/orders")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public MockSupplierOrderResponse createOrder(@RequestBody MockSupplierOrderRequest request) {
        return mockSupplierService.createOrder(request);
    }
}
