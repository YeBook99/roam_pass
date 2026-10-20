package com.roampass.payment.controller;

import com.roampass.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

// PG 승인 통지(웹훅). 서명이 틀리면 401, 본문을 읽지 못하면 400 (GlobalExceptionHandler가 응답을 만든다)
@RestController
@RequiredArgsConstructor
public class PgWebhookController {

    private final PaymentService paymentService;

    // 본문을 DTO가 아니라 byte[]로 받는다. 서명은 받은 원문 그대로 검증해야 하기 때문이다
    // 처리(커밋)가 끝난 뒤에 200을 준다. 실패하면 PG가 다시 보낸다
    @PostMapping("/api/webhooks/pg")
    public void receive(@RequestBody byte[] body,
                        @RequestHeader(value = "X-Signature", required = false) String signature) {
        paymentService.receiveWebhook(body, signature);
    }
}
