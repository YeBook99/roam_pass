package com.roampass.payment.service;

import com.roampass.history.domain.Signal;
import com.roampass.infra.pg.PgPaymentResult;
import com.roampass.payment.domain.PaymentConfirmResult;

// PG 결과를 우리 DB에 반영하는 유일한 경로. 승인 API 응답·웹훅·대사가 모두 여기로 들어온다 (docs/05 §3)
// 세 경로가 같이 쓰는 use case라 PaymentService와 다른 빈으로 둔다 (ARCHITECTURE.md §7-2)
public interface PaymentConfirmService {

    // 중복 기록과 상태 변경이 한 트랜잭션이다. 처리 중 실패하면 중복 기록도 같이 롤백되어 다음 재전송이 정상 처리된다
    PaymentConfirmResult confirm(PgPaymentResult result, Signal signal);

    // 대사에서 PG에 승인 기록이 없다고 확인된 결제
    boolean failWithoutPgRecord(Long paymentId);
}
