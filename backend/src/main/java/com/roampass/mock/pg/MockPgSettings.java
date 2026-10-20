package com.roampass.mock.pg;

// 시연용 모의 PG 동작 설정. PUT /mock/pg/config로 바꾸고 POST /mock/reset으로 되돌린다
public record MockPgSettings(
        boolean failApprove,        // true면 승인 대신 거절
        long responseDelayMs,       // 승인 응답을 늦춘다. 3000ms를 넘기면 우리 쪽은 타임아웃 ("승인됐는데 응답 못 받음")
        int webhookCount,           // 웹훅을 몇 번 보낼지. 0 = 누락, 2 이상 = 중복
        long webhookDelayMs,        // 승인 후 웹훅까지의 지연
        int webhookAmountDelta,     // 웹훅 금액에 더할 값. 0이 아니면 금액이 틀린 통지
        double cancelFailRate       // 취소 요청을 500으로 실패시킬 확률 (0~1)
) {

    static final MockPgSettings DEFAULT = new MockPgSettings(false, 0, 1, 500, 0, 0);

    // PUT 본문에 온 값만 바꾼다
    public record Patch(Boolean failApprove, Long responseDelayMs, Integer webhookCount,
                        Long webhookDelayMs, Integer webhookAmountDelta, Double cancelFailRate) {
    }

    MockPgSettings apply(Patch p) {
        return new MockPgSettings(
                p.failApprove() != null ? p.failApprove() : failApprove,
                p.responseDelayMs() != null ? p.responseDelayMs() : responseDelayMs,
                p.webhookCount() != null ? p.webhookCount() : webhookCount,
                p.webhookDelayMs() != null ? p.webhookDelayMs() : webhookDelayMs,
                p.webhookAmountDelta() != null ? p.webhookAmountDelta() : webhookAmountDelta,
                p.cancelFailRate() != null ? p.cancelFailRate() : cancelFailRate);
    }
}