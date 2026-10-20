package com.roampass.mock.pg;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

// 모의 PG API. 시연용이라 mock.enabled=true일 때만 켜진다
@RestController
@ConditionalOnProperty(name = "mock.enabled", havingValue = "true")
class MockPgController {

    record ApproveRequest(String merchantRequestId, String orderNo, int amount) {
    }

    record CancelRequest(String pgTid, String requestKey, int amount) {
    }

    private final MockPgService service;
    private final MockPgWebhookSender webhookSender;

    MockPgController(MockPgService service, MockPgWebhookSender webhookSender) {
        this.service = service;
        this.webhookSender = webhookSender;
    }

    // 승인을 저장하고 웹훅을 예약한 뒤, 설정만큼 늦게 응답한다
    // 응답 지연이 우리 타임아웃(3초)보다 길면 "PG는 승인했는데 우리는 응답을 못 받은" 상황이 된다
    @PostMapping("/mock/pg/approve")
    MockPgResult approve(@RequestBody ApproveRequest req) throws InterruptedException {
        MockPgSettings s = service.settings();
        MockPgResult result = service.approve(req.merchantRequestId(), req.orderNo(), req.amount());
        webhookSender.schedule(result, s);
        Thread.sleep(s.responseDelayMs());
        return result;
    }

    @GetMapping("/mock/pg/payments/{merchantRequestId}")
    ResponseEntity<MockPgResult> find(@PathVariable String merchantRequestId) {
        return ResponseEntity.of(service.find(merchantRequestId));
    }

    @PostMapping("/mock/pg/cancel")
    MockPgService.CancelResult cancel(@RequestBody CancelRequest req) {
        return service.cancel(req.pgTid(), req.requestKey(), req.amount());
    }

    @GetMapping("/mock/pg/config")
    MockPgSettings config() {
        return service.settings();
    }

    @PutMapping("/mock/pg/config")
    MockPgSettings updateConfig(@RequestBody MockPgSettings.Patch patch) {
        return service.update(patch);
    }

    @PostMapping("/mock/reset")
    MockPgSettings reset() {
        service.reset();
        return service.settings();
    }
}