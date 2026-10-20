package com.roampass.mock.pg;

// 모의 PG가 돌려주는 승인 결과 (승인 응답·웹훅·조회 공통 형식)
public record MockPgResult(String merchantRequestId, String pgTid, String orderNo, int amount, String status) {
}