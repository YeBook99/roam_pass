package com.roampass.infra.pg;

// PG가 알려 주는 승인 결과를 우리 쪽 형태로 바꾼 것. 승인 응답·웹훅·대사 조회가 모두 이 모양이다
// merchantRequestId = 우리가 승인 요청 때 보낸 payment.id / rawPayload = 받은 원문 (callback_log에 그대로 남긴다)
public record PgPaymentResult(String merchantRequestId, String pgTid, String orderNo, int amount, String status,
                              String rawPayload) {

    public boolean approved() {
        return "APPROVED".equals(status);
    }
}
