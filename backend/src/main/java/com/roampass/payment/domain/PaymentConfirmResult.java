package com.roampass.payment.domain;

// PG 결과를 반영한 결과. 이 이름이 callback_log.result에 그대로 남는다
public enum PaymentConfirmResult {
    PROCESSED,          // 처리함
    DUPLICATE,          // 같은 통지가 이미 처리됨 (callback_log에는 남기지 않는다)
    UNMATCHED,          // 우리 결제를 찾을 수 없음
    AMOUNT_MISMATCH,    // 금액이 주문 금액과 다름 → 결제 완료로 처리하지 않음
    STALE_IGNORED,      // 결제가 이미 READY가 아님
    ORDER_NOT_PENDING   // 결제는 승인됐지만 주문이 이미 PENDING이 아님(예: 만료). 자동 취소는 만료 스케줄러와 함께 추가
}
