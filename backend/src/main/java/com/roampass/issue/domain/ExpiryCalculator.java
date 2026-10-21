package com.roampass.issue.domain;

import java.time.Duration;
import java.time.Instant;

// 만료일 계산 (docs/07 §4). "7일" = 24시간 × 7. 날짜 7개(달력 기준)가 아니다
// 해외에 있는 사용자에게 어느 나라 시간으로 봐도 똑같이 168시간이 되도록 시간 단위로 계산한다
public final class ExpiryCalculator {

    private ExpiryCalculator() {
    }

    // activatedAt은 공급사가 준 UTC 시각. 결과도 UTC 시각이고, KST 표시는 화면에서 한다
    public static Instant expiresAt(Instant activatedAt, int days) {
        return activatedAt.plus(Duration.ofHours(24L * days));
    }
}
