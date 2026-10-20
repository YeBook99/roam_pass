package com.roampass.mockpg.controller;

import com.roampass.mockpg.dto.MockPgApproveRequest;
import com.roampass.mockpg.dto.MockPgCancelRequest;
import com.roampass.mockpg.dto.MockPgCancelResponse;
import com.roampass.mockpg.dto.MockPgPaymentResponse;
import com.roampass.mockpg.dto.MockPgSettings;
import com.roampass.mockpg.dto.MockPgSettingsPatch;
import com.roampass.mockpg.service.MockPgService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

// 모의 PG API. 시연용이라 mock.enabled=true일 때만 켜진다
@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mock.enabled", havingValue = "true")
public class MockPgController {

    private final MockPgService mockPgService;

    @PostMapping("/mock/pg/approve")
    public MockPgPaymentResponse approve(@RequestBody MockPgApproveRequest request) {
        return mockPgService.approve(request);
    }

    @GetMapping("/mock/pg/payments/{merchantRequestId}")
    public MockPgPaymentResponse getPayment(@PathVariable String merchantRequestId) {
        return mockPgService.getPayment(merchantRequestId);
    }

    @PostMapping("/mock/pg/cancel")
    public MockPgCancelResponse cancel(@RequestBody MockPgCancelRequest request) {
        return mockPgService.cancel(request);
    }

    @GetMapping("/mock/pg/config")
    public MockPgSettings getSettings() {
        return mockPgService.getSettings();
    }

    @PutMapping("/mock/pg/config")
    public MockPgSettings updateSettings(@RequestBody MockPgSettingsPatch patch) {
        return mockPgService.updateSettings(patch);
    }

    @PostMapping("/mock/reset")
    public MockPgSettings reset() {
        return mockPgService.resetSettings();
    }
}
