package com.roampass.infra.pg;

import java.util.Optional;

// Service가 의존하는 것은 이 인터페이스다. PG가 바뀌어도 Service는 그대로다 (ARCHITECTURE.md §13-2)
public interface PgClient {

    // 승인 요청. 타임아웃·5xx면 empty = "결과를 모름"(실패가 아니다). 호출한 쪽은 READY로 두고 웹훅·대사가 확정한다
    Optional<PgPaymentResult> approve(long paymentId, String orderNo, int amount);

    // 대사용 조회. PG에 기록이 없으면 empty. PG가 응답하지 않으면 BusinessException(EXTERNAL_*)
    Optional<PgPaymentResult> find(long paymentId);

    // 웹훅 본문(외부 형식)을 우리 결과로 바꾼다. 읽을 수 없으면 empty. 서명 검증은 이 전에 끝나 있어야 한다
    Optional<PgPaymentResult> parseNotification(byte[] body);
}
