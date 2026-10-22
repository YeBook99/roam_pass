package com.roampass.payment.service;

import com.roampass.payment.dto.PaymentResponse;

// 결제 요청과 PG 웹훅 수신. 두 메서드 모두 외부 호출이나 여러 트랜잭션이 섞여 @Transactional을 붙이지 않는다 (ARCHITECTURE.md §7)
public interface PaymentService {

    // Idempotency-Key: 화면이 결제 페이지에 들어올 때 한 번 만든 UUID. 연타·재시도에도 같은 값을 보낸다
    PaymentResponse pay(String orderNo, String userId, String idempotencyKey);

    // 서명을 먼저 검증하고, 검증 전에는 본문을 우리 객체로 바꾸지 않는다 (ARCHITECTURE.md §13-7, CLAUDE.md §3-4)
    // 처리하지 못한 통지도 callback_log에 남긴 뒤 예외를 던진다 (기록은 별도 트랜잭션으로 이미 커밋됨)
    void receiveWebhook(byte[] body, String signature);
}
