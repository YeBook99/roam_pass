package com.roampass.payment;

// PG가 알려 주는 승인 결과. 승인 API 응답, 웹훅, 대사 조회가 모두 같은 모양이다
// merchantRequestId = 우리가 승인 요청 때 보낸 payment.id
public record PgResult(String merchantRequestId, String pgTid, String orderNo, int amount, String status) {

    public boolean approved() {
        return "APPROVED".equals(status);
    }
}