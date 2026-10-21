package com.roampass.mocksupplier.controller;

import com.roampass.mocksupplier.dto.MockSupplierOrderRequest;
import com.roampass.mocksupplier.dto.MockSupplierOrderResponse;
import com.roampass.mocksupplier.dto.MockSupplierProfileResponse;
import com.roampass.mocksupplier.dto.MockSupplierStateRequest;
import com.roampass.mocksupplier.service.MockSupplierService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    // 프로필 상태 조회 (우리 서버가 설치·개통 여부를 물어보는 곳)
    @GetMapping("/mock/supplier/profiles/{iccid}")
    public MockSupplierProfileResponse getProfile(@PathVariable String iccid) {
        return mockSupplierService.getProfile(iccid);
    }

    // 시연용: 실제 폰 대신 공급사 상태를 바꿔 설치·개통을 흉내 낸다 (과제 2.2 허용)
    @PostMapping("/mock/supplier/profiles/{iccid}/state")
    public MockSupplierProfileResponse changeState(@PathVariable String iccid,
                                                   @RequestBody MockSupplierStateRequest request) {
        return mockSupplierService.changeState(iccid, request);
    }
}
